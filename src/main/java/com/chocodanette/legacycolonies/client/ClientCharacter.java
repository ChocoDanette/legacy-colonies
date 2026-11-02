package com.chocodanette.legacycolonies.client;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.network.CharacterOpenPayload;
import com.chocodanette.legacycolonies.network.CharacterSheetPayload;
import com.ldtteam.blockui.BOScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Opens the character window with its key (P by default, in the controls' "Legacy" category), in game or from the
 * inventory. The server sends the window's content, which opens it.
 */
@EventBusSubscriber(modid = LegacyColonies.MOD_ID, value = Dist.CLIENT)
public final class ClientCharacter
{
    public static final KeyMapping KEY = new KeyMapping("key.legacycolonies.character", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
      GLFW.GLFW_KEY_P, "key.categories.legacycolonies");

    private ClientCharacter()
    {
    }

    public static void show(final CharacterSheetPayload sheet)
    {
        if (Minecraft.getInstance().screen instanceof final BOScreen screen && screen.getWindow() instanceof final WindowCharacter window)
        {
            window.update(sheet);
            return;
        }
        new WindowCharacter(sheet).open();
    }

    public static void request()
    {
        PacketDistributor.sendToServer(new CharacterOpenPayload());
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event)
    {
        final Minecraft minecraft = Minecraft.getInstance();
        while (KEY.consumeClick())
        {
            if (minecraft.player != null && minecraft.screen == null)
            {
                request();
            }
        }
    }

    /**
     * The key also opens the window from the inventory (its in-game conflict context is not active in a screen).
     */
    @SubscribeEvent
    public static void onScreenKey(final ScreenEvent.KeyPressed.Post event)
    {
        if (event.getScreen() instanceof InventoryScreen && KEY.matches(event.getKeyCode(), event.getScanCode()))
        {
            request();
        }
    }

    @EventBusSubscriber(modid = LegacyColonies.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Keys
    {
        private Keys()
        {
        }

        @SubscribeEvent
        public static void onRegisterKeys(final RegisterKeyMappingsEvent event)
        {
            event.register(KEY);
        }
    }
}
