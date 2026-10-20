package com.chocodanette.legacycolonies.validation;

import com.chocodanette.legacycolonies.ModBlocks;
import com.minecolonies.api.blocks.AbstractBlockHut;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Right click a hut with the builder's register to validate the building around it.
 */
public final class RegisterHandler
{
    private RegisterHandler()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event)
    {
        if (!event.getItemStack().is(ModBlocks.REGISTER.get())
              || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof AbstractBlockHut<?>))
        {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel() instanceof final ServerLevel level && event.getEntity() instanceof final ServerPlayer player)
        {
            BuildingValidator.validate(player, level, event.getPos());
        }
    }
}
