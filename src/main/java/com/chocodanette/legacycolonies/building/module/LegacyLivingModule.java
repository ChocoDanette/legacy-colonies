package com.chocodanette.legacycolonies.building.module;

import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.modules.BedHandlingModule;
import com.minecolonies.core.colony.buildings.modules.LivingBuildingModule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.List;

/**
 * Living module of the Legacy huts: one citizen per bed. Only the beds registered in the building (placed by the builder,
 * or found when the building was confirmed) count, up to a number set by the building's level.
 * <p>
 * MineColonies forgets a registered bed that a citizen finds outside the {@code minecraft:beds} tag (the TFC thatch bed
 * before the modpack tagged it), and a bed placed by a player is never registered: a building short of beds looks for
 * the beds standing inside it and registers them again.
 */
public abstract class LegacyLivingModule extends LivingBuildingModule
{
    /**
     * Ticks between two looks for unregistered beds.
     */
    private static final long SCAN_INTERVAL = 200;

    private long nextScan;

    /**
     * Most beds the building may use at a level.
     */
    protected abstract int maxBeds(int level);

    @Override
    public int getModuleMax()
    {
        final int level = building.getBuildingLevel();
        if (level <= 0)
        {
            return 0;
        }
        int standing = standingBeds();
        if (standing < maxBeds(level) && registerBedsInside())
        {
            standing = standingBeds();
        }
        return Math.min(maxBeds(level), standing);
    }

    /**
     * Register the bed heads inside the building's corners that are not registered yet.
     *
     * @return true when a bed was registered.
     */
    private boolean registerBedsInside()
    {
        final BedHandlingModule beds = building.getModule(BedHandlingModule.class);
        if (beds == null || !(building.getColony().getWorld() instanceof final ServerLevel world) || building.getCorners() == null)
        {
            return false;
        }
        if (world.getGameTime() < nextScan)
        {
            return false;
        }
        nextScan = world.getGameTime() + SCAN_INTERVAL;
        final BlockPos min = building.getCorners().getA();
        final BlockPos max = building.getCorners().getB();
        if (!WorldUtil.isBlockLoaded(world, min) || !WorldUtil.isBlockLoaded(world, max))
        {
            return false;
        }
        final List<BlockPos> registered = beds.getRegisteredBlocks();
        boolean added = false;
        for (final BlockPos pos : BlockPos.betweenClosed(min, max))
        {
            final BlockState state = world.getBlockState(pos);
            if (isBed(state) && state.hasProperty(BedBlock.PART) && state.getValue(BedBlock.PART) == BedPart.HEAD
                  && !registered.contains(pos))
            {
                beds.onBlockPlacedInBuilding(state, pos.immutable(), world);
                added = true;
            }
        }
        if (added)
        {
            building.markDirty();
        }
        return added;
    }

    /**
     * Registered beds still in place. A bed in an unloaded chunk is taken as standing.
     */
    private int standingBeds()
    {
        final BedHandlingModule beds = building.getModule(BedHandlingModule.class);
        if (beds == null)
        {
            return 0;
        }
        final Level world = building.getColony().getWorld();
        int count = 0;
        for (final BlockPos pos : beds.getRegisteredBlocks())
        {
            if (world == null || !WorldUtil.isBlockLoaded(world, pos) || isBed(world.getBlockState(pos)))
            {
                count++;
            }
        }
        return count;
    }

    private static boolean isBed(final BlockState state)
    {
        return state.getBlock() instanceof BedBlock || state.is(BlockTags.BEDS);
    }
}
