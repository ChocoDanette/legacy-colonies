package com.chocodanette.legacycolonies.client;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.network.FirepitFoundingPayload;
import com.chocodanette.legacycolonies.network.FoundColonyPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * "Found a colony here" button above the TFC firepit window. It is added hidden when the window opens, and shown when the
 * server says the player may found a colony at this firepit ({@link FirepitFoundingPayload}, sent just after the window
 * opens). A click asks for confirmation first.
 */
@EventBusSubscriber(modid = LegacyColonies.MOD_ID, value = Dist.CLIENT)
public final class ClientFounding
{
    private static final String FIREPIT_SCREEN = "net.dries007.tfc.client.screen.FirepitScreen";
    private static final int    BUTTON_WIDTH   = 140;

    private static int      allowedContainer = -1;
    @Nullable
    private static BlockPos allowedFirepit;
    @Nullable
    private static Button   button;
    private static int      buttonContainer  = -1;

    private ClientFounding()
    {
    }

    @SubscribeEvent
    public static void onScreenInit(final ScreenEvent.Init.Post event)
    {
        if (!(event.getScreen() instanceof final AbstractContainerScreen<?> screen) || !screen.getClass().getName().equals(FIREPIT_SCREEN))
        {
            return;
        }
        buttonContainer = screen.getMenu().containerId;
        button = Button.builder(Component.translatable("com.legacycolonies.hearth.found"), b -> askConfirmation())
                   .bounds(screen.getGuiLeft() + (screen.getXSize() - BUTTON_WIDTH) / 2, screen.getGuiTop() - 24, BUTTON_WIDTH, 20)
                   .build();
        button.visible = buttonContainer == allowedContainer;
        event.addListener(button);
    }

    public static void allow(final FirepitFoundingPayload payload)
    {
        allowedContainer = payload.containerId();
        allowedFirepit = payload.firepit();
        if (button != null && buttonContainer == allowedContainer)
        {
            button.visible = true;
        }
    }

    private static void askConfirmation()
    {
        final Minecraft minecraft = Minecraft.getInstance();
        final BlockPos firepit = allowedFirepit;
        if (minecraft.player == null || firepit == null)
        {
            return;
        }
        allowedContainer = -1;
        minecraft.player.closeContainer();
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed)
            {
                PacketDistributor.sendToServer(new FoundColonyPayload(firepit));
            }
            minecraft.setScreen(null);
        }, Component.translatable("com.legacycolonies.hearth.confirm.title"), Component.translatable("com.legacycolonies.hearth.confirm.text")));
    }
}
