package com.chocodanette.legacycolonies.colony;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.ModBlocks;
import com.chocodanette.legacycolonies.block.BlockColonyCampfire;
import com.chocodanette.legacycolonies.network.FirepitFoundingPayload;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The colony hearth is not an item the player can make: the window of a TFC firepit shows a "found a colony here" button
 * (see {@code ClientFounding}) when the player has no colony and the firepit is outside every colony. After the player
 * confirms, the firepit becomes the colony hearth, which founds the colony there.
 */
public final class HearthConversionHandler
{
    private static final ResourceLocation FIREPIT           = ResourceLocation.fromNamespaceAndPath("tfc", "firepit");
    private static final String           FIREPIT_CONTAINER = "net.dries007.tfc.common.container.FirepitContainer";

    /**
     * Farther than this, the player cannot have the firepit window open.
     */
    private static final double MAX_DISTANCE_SQ = 8 * 8;

    private HearthConversionHandler()
    {
    }

    /**
     * Whether the player may found a colony at this firepit: no colony there and none owned by the player.
     */
    public static boolean canFound(final Level level, final BlockPos pos, final ServerPlayer player)
    {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(FIREPIT)
                 && IColonyManager.getInstance().getIColony(level, pos) == null
                 && IColonyManager.getInstance().getIColonyByOwner(level, player) == null;
    }

    /**
     * When a firepit window opens, tells the client whether to show the button. The block is read from the TFC menu by
     * reflection, so that the addon does not depend on TFC.
     */
    @SubscribeEvent
    public static void onContainerOpen(final PlayerContainerEvent.Open event)
    {
        final AbstractContainerMenu menu = event.getContainer();
        if (!(event.getEntity() instanceof final ServerPlayer player) || !menu.getClass().getName().equals(FIREPIT_CONTAINER))
        {
            return;
        }
        final BlockPos pos = firepitPos(menu);
        if (pos != null && canFound(player.serverLevel(), pos, player))
        {
            PacketDistributor.sendToPlayer(player, new FirepitFoundingPayload(menu.containerId, pos));
        }
    }

    @Nullable
    private static BlockPos firepitPos(final AbstractContainerMenu menu)
    {
        try
        {
            return menu.getClass().getMethod("getBlockEntity").invoke(menu) instanceof final BlockEntity blockEntity ? blockEntity.getBlockPos() : null;
        }
        catch (final ReflectiveOperationException e)
        {
            LegacyColonies.LOGGER.warn("Cannot read the firepit of a TFC firepit window", e);
            return null;
        }
    }

    /**
     * Sent by the confirmed button: the firepit drops what it holds, then the colony hearth takes its place.
     */
    public static void found(final ServerPlayer player, final BlockPos pos)
    {
        final Level level = player.serverLevel();
        if (player.blockPosition().distSqr(pos) > MAX_DISTANCE_SQ || !level.isLoaded(pos))
        {
            return;
        }
        if (!canFound(level, pos, player))
        {
            player.displayClientMessage(Component.translatable("com.legacycolonies.hearth.refused"), true);
            return;
        }
        player.closeContainer();
        level.removeBlock(pos, false);
        final BlockColonyCampfire hearth = ModBlocks.COLONY_CAMPFIRE.get();
        HutPlacementHandler.placeHut(level, pos, player, hearth, null, new ItemStack(hearth));
    }
}
