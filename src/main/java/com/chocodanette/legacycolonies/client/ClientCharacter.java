package com.chocodanette.legacycolonies.client;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.network.CharacterOpenPayload;
import com.chocodanette.legacycolonies.network.CharacterSheetPayload;
import com.ldtteam.blockui.BOScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
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
 * Opens the character window: with its key (P by default, in the controls' "Legacy" category) or with the "Character"
 * button above the inventory. The server sends the window's content, which opens it.
 */
@EventBusSubscriber(modid = LegacyColonies.MOD_ID, value = Dist.CLIENT)
public final class ClientCharacter
{
    public static final KeyMapping KEY = new KeyMapping("key.legacycolonies.character", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
      GLFW.GLFW_KEY_P, "key.categories.legacycolonies");

    private static final int BUTTON_WIDTH  = 70;
    private static final int BUTTON_HEIGHT = 16;

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

    @SubscribeEvent
    public static void onScreenInit(final ScreenEvent.Init.Post event)
    {
        if (event.getScreen() instanceof final InventoryScreen screen)
        {
            event.addListener(new InventoryButton(screen));
        }
    }

    /**
     * Above the inventory's top left corner. It follows the inventory when the recipe book moves it.
     */
    private static final class InventoryButton extends Button
    {
        private final InventoryScreen screen;

        private InventoryButton(final InventoryScreen screen)
        {
            super(screen.getGuiLeft(), screen.getGuiTop() - BUTTON_HEIGHT - 2, BUTTON_WIDTH, BUTTON_HEIGHT,
              Component.translatable("com.legacycolonies.character.button"), button -> request(), DEFAULT_NARRATION);
            this.screen = screen;
        }

        @Override
        protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick)
        {
            setPosition(screen.getGuiLeft(), screen.getGuiTop() - BUTTON_HEIGHT - 2);
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
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
