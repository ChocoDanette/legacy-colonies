package com.chocodanette.legacycolonies.material;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.permissions.Action;
import com.chocodanette.legacycolonies.network.MaterialChoicesPayload;
import com.minecolonies.api.eventbus.events.colony.buildings.BuildingRemovedModEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Right click on a hut with a log or a mud brick block to choose that material for the building.
 */
public final class MaterialChoiceHandler
{
    private MaterialChoiceHandler()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event)
    {
        final ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof final BlockItem blockItem)
              || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof AbstractBlockHut<?>))
        {
            return;
        }

        final ResourceLocation heldId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        for (final MaterialSlot slot : MaterialSlot.values())
        {
            final ResourceLocation material = slot.choiceOf(heldId);
            if (material == null)
            {
                continue;
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getLevel() instanceof final ServerLevel level)
            {
                final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, event.getPos());
                if (colony != null && !colony.getPermissions().hasPermission(event.getEntity(), Action.ACCESS_HUTS))
                {
                    return;
                }
                MaterialChoices.get(level).choose(event.getPos(), slot, material);
                MaterialChoicesPayload.sendToLevel(level);
                event.getEntity().displayClientMessage(
                  Component.translatable("com.legacycolonies.material.chosen." + slot.getId(), stack.getHoverName()), true);
            }
            return;
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof final ServerPlayer player)
        {
            MaterialChoicesPayload.sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof final ServerPlayer player)
        {
            MaterialChoicesPayload.sendTo(player);
        }
    }

    /**
     * Forget the materials of a building once it is removed.
     */
    public static void onBuildingRemoved(final BuildingRemovedModEvent event)
    {
        if (event.getBuilding().getColony().getWorld() instanceof final ServerLevel level)
        {
            MaterialChoices.get(level).clear(event.getBuilding().getPosition());
            MaterialChoicesPayload.sendToLevel(level);
        }
    }
}
