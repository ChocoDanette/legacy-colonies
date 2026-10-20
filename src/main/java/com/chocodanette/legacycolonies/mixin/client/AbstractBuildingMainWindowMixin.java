package com.chocodanette.legacycolonies.mixin.client;

import com.chocodanette.legacycolonies.network.ConfirmBuildingPayload;
import com.chocodanette.legacycolonies.validation.ClientBuildingRules;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.views.View;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.client.gui.AbstractBuildingMainWindow;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a "confirm building" button next to the hut's build button when the hut's next level can be built by hand.
 */
@Mixin(value = AbstractBuildingMainWindow.class, remap = false)
public abstract class AbstractBuildingMainWindowMixin
{
    private static final String CONFIRM_ID = "legacycolonies_confirm";

    @Shadow
    @Final
    private Button buttonBuild;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void legacycolonies$addConfirmButton(final IBuildingView buildingView, final ResourceLocation resource, final CallbackInfo ci)
    {
        final int nextLevel = buildingView.getBuildingLevel() + 1;
        if (buttonBuild == null || buttonBuild.getParent() == null
              || nextLevel > buildingView.getBuildingMaxLevel()
              || !ClientBuildingRules.canConfirm(buildingView.getBuildingType().getRegistryName(), nextLevel))
        {
            return;
        }

        final View parent = buttonBuild.getParent();
        final ButtonImage confirm = new ButtonImage();
        confirm.setID(CONFIRM_ID);
        confirm.setImage(ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/gui/builderhut/builder_button_medium_large.png"));
        confirm.setSize(buttonBuild.getWidth(), buttonBuild.getHeight());
        confirm.setColors(0);
        confirm.setText(Component.translatable("com.legacycolonies.confirm.button"));
        confirm.setPosition(buttonBuild.getX(), freeRow(parent, buttonBuild));
        parent.addChild(confirm);

        ((AbstractWindowSkeleton) (Object) this).registerButton(CONFIRM_ID, () -> {
            PacketDistributor.sendToServer(new ConfirmBuildingPayload(buildingView.getPosition()));
            ((AbstractWindowSkeleton) (Object) this).close();
        });
    }

    /**
     * The row just below the build button, or just above it when something is already there.
     */
    private static int freeRow(final View parent, final Button build)
    {
        final int step = build.getHeight() + 1;
        final int below = build.getY() + step;
        return isFree(parent, build, below) || !isFree(parent, build, build.getY() - step) ? below : build.getY() - step;
    }

    private static boolean isFree(final View parent, final Button build, final int y)
    {
        for (final Pane pane : parent.getChildren())
        {
            if (pane != build && pane.isVisible()
                  && pane.getX() < build.getX() + build.getWidth() && pane.getX() + pane.getWidth() > build.getX()
                  && pane.getY() < y + build.getHeight() && pane.getY() + pane.getHeight() > y)
            {
                return false;
            }
        }
        return true;
    }
}
