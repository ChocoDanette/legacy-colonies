package com.chocodanette.legacycolonies.client;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.colony.HutPlacementHandler;
import com.ldtteam.structurize.client.gui.WindowExtendedBuildTool;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Right click the air with a hut to open the build tool, as if holding it.
 */
@EventBusSubscriber(modid = LegacyColonies.MOD_ID, value = Dist.CLIENT)
public final class ClientHutHandler
{
    private ClientHutHandler()
    {
    }

    @SubscribeEvent
    public static void onRightClickItem(final PlayerInteractEvent.RightClickItem event)
    {
        if (!event.getLevel().isClientSide || !HutPlacementHandler.isPlaceableHut(event.getItemStack()))
        {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (Minecraft.getInstance().screen == null)
        {
            new WindowExtendedBuildTool(null, 1, null, WindowExtendedBuildTool.BLOCK_BLUEPRINT_REQUIREMENT, event.getLevel().registryAccess()).open();
        }
    }
}
