package com.chocodanette.legacycolonies.needs;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.util.EntityUtils;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * New citizens come from outside: one every few game days, at the edge of the colony, while the colony has room for
 * them in its era (see {@link ColonyNeeds#allowedCitizens}). They walk in on their own. MineColonies still brings the
 * first citizen, also from the edge (see {@code CitizenManagerMixin}).
 */
public final class ColonyArrivals
{
    private static final int  CHECK_INTERVAL = 100;
    private static final long TICKS_PER_DAY  = 24000;
    /**
     * Distance kept between the arrival point and the border of the colony's first claim, in blocks.
     */
    private static final int  EDGE_MARGIN    = 8;
    private static final int  EDGE_TRIES     = 16;

    private static boolean errorLogged = false;

    private ColonyArrivals()
    {
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event)
    {
        if (event.getServer().getTickCount() % CHECK_INTERVAL != 0)
        {
            return;
        }
        for (final IColony colony : IColonyManager.getInstance().getAllColonies())
        {
            try
            {
                tick(colony);
            }
            catch (final RuntimeException e)
            {
                if (!errorLogged)
                {
                    LegacyColonies.LOGGER.error("Colony arrivals failed for colony {}", colony.getID(), e);
                    errorLogged = true;
                }
            }
        }
    }

    private static void tick(final IColony colony)
    {
        if (!(colony.getWorld() instanceof final ServerLevel level)
              || !colony.getServerBuildingManager().hasTownHall()
              || !colony.getSettings().getSetting(BuildingTownHall.MOVE_IN).getValue())
        {
            return;
        }
        final int count = colony.getCitizenManager().getCurrentCitizenCount();
        // MineColonies brings the first citizens itself.
        if (count < IMinecoloniesAPI.getInstance().getConfig().getServer().initialCitizenAmount.get())
        {
            return;
        }

        final ColonyEras data = ColonyEras.get(level);
        final int allowed = ColonyNeeds.allowedCitizens(colony);
        if (count >= allowed)
        {
            data.setNextArrival(colony.getID(), 0);
            return;
        }

        final long now = level.getGameTime();
        final long interval = Math.max(CHECK_INTERVAL, Math.round(ColonyNeeds.rules(colony).arrivalDays() * TICKS_PER_DAY));
        final long next = data.getNextArrival(colony.getID());
        if (next == 0)
        {
            data.setNextArrival(colony.getID(), now + interval);
            return;
        }
        if (now < next)
        {
            return;
        }

        final BlockPos spawn = findEdgeSpawn(colony);
        if (spawn == null)
        {
            // The edge is not loaded: the citizen comes when a player is around.
            return;
        }
        arrive(colony, level, spawn);
        data.setNextArrival(colony.getID(), count + 1 < allowed ? now + interval : 0);
    }

    /**
     * Seconds until the next citizen arrives, or -1 when none is expected.
     */
    public static long secondsToNextArrival(final IColony colony)
    {
        if (!(colony.getWorld() instanceof final ServerLevel level))
        {
            return -1;
        }
        final long next = ColonyEras.get(level).getNextArrival(colony.getID());
        return next == 0 ? -1 : Math.max(0, (next - level.getGameTime()) / 20);
    }

    private static void arrive(final IColony colony, final ServerLevel level, final BlockPos spawn)
    {
        final List<ICitizenData> citizens = colony.getCitizenManager().getCitizens();
        int females = 0;
        for (final ICitizenData citizen : citizens)
        {
            females += citizen.isFemale() ? 1 : 0;
        }
        final ICitizenData citizen = colony.getCitizenManager().createAndRegisterCivilianData();
        // Like MineColonies: the new citizen takes the less common gender.
        citizen.setGenderAndGenerateName(females < citizens.size() / 2.0);
        colony.getCitizenManager().spawnOrCreateCivilian(citizen, level, List.of(spawn), true);
        MessageUtils.format(Component.translatable("com.legacycolonies.needs.arrived", citizen.getName())).sendTo(colony).forAllPlayers();
        LegacyColonies.LOGGER.info("Citizen {} arrives in colony {} at {}", citizen.getName(), colony.getID(), spawn);
    }

    /**
     * A spot on dry ground near the border of the colony's first claim, in a random direction, or null when none of the
     * tried spots is loaded and fit to stand on.
     */
    @Nullable
    public static BlockPos findEdgeSpawn(final IColony colony)
    {
        if (!(colony.getWorld() instanceof final ServerLevel level))
        {
            return null;
        }
        final BlockPos center = colony.getCenter();
        final int radius = Math.max(1, IMinecoloniesAPI.getInstance().getConfig().getServer().initialColonySize.get() * 16 - EDGE_MARGIN);
        final RandomSource random = level.getRandom();
        for (int i = 0; i < EDGE_TRIES; i++)
        {
            final double angle = random.nextDouble() * Math.PI * 2;
            // Every other try a chunk closer: the claim is counted in whole chunks around the centre's chunk.
            final int distance = Math.max(EDGE_MARGIN, radius - (i % 2) * 16);
            final int x = center.getX() + (int) Math.round(Math.cos(angle) * distance);
            final int z = center.getZ() + (int) Math.round(Math.sin(angle) * distance);
            final BlockPos column = new BlockPos(x, center.getY(), z);
            if (!WorldUtil.isEntityBlockLoaded(level, column))
            {
                continue;
            }
            final BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.below()).isEmpty())
            {
                continue;
            }
            final IColony owner = IColonyManager.getInstance().getColonyByPosFromWorld(level, ground);
            if (owner == null || owner.getID() != colony.getID())
            {
                continue;
            }
            final BlockPos spawn = EntityUtils.getSpawnPoint(level, ground);
            if (spawn != null)
            {
                return spawn;
            }
        }
        return null;
    }
}
