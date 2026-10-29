package com.chocodanette.legacycolonies.mixin.client;

import com.chocodanette.legacycolonies.ModBuildings;
import com.chocodanette.legacycolonies.colony.BuildMode;
import com.chocodanette.legacycolonies.colony.ClientBuildModes;
import com.chocodanette.legacycolonies.network.ConfirmBuildingPayload;
import com.chocodanette.legacycolonies.network.SchoolOpenPayload;
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
 * Once the hut has a build mode, only its button stays: a hut given to the builder loses "confirm building", a hut built
 * by hand loses the build button (and takes its place).
 * A built school also gets an "heir" button, which opens its heir window.
 */
@Mixin(value = AbstractBuildingMainWindow.class, remap = false)
public abstract class AbstractBuildingMainWindowMixin
{
    private static final String CONFIRM_ID = "legacycolonies_confirm";
    private static final String HEIR_ID = "legacycolonies_heir";

    @Shadow
    @Final
    private Button buttonBuild;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void legacycolonies$addButtons(final IBuildingView buildingView, final ResourceLocation resource, final CallbackInfo ci)
    {
        if (buttonBuild == null || buttonBuild.getParent() == null)
        {
            return;
        }
        legacycolonies$addConfirmButton(buildingView);
        if (buildingView.getBuildingType() == ModBuildings.SCHOOL.get() && buildingView.getBuildingLevel() >= 1)
        {
            legacycolonies$addHeirButton(buildingView);
        }
    }

    private void legacycolonies$addConfirmButton(final IBuildingView buildingView)
    {
        final BuildMode mode = ClientBuildModes.getMode(buildingView.getPosition());
        if (mode == BuildMode.HAND)
        {
            buttonBuild.setVisible(false);
        }

        final int nextLevel = buildingView.getBuildingLevel() + 1;
        if (mode == BuildMode.BUILDER
              || nextLevel > buildingView.getBuildingMaxLevel()
              || !ClientBuildingRules.canConfirm(buildingView.getBuildingType().getRegistryName(), nextLevel))
        {
            return;
        }

        final View parent = buttonBuild.getParent();
        final ButtonImage confirm = legacycolonies$newButton(CONFIRM_ID, Component.translatable("com.legacycolonies.confirm.button"));
        confirm.setPosition(buttonBuild.getX(), mode == BuildMode.HAND ? buttonBuild.getY() : freeRow(parent, buttonBuild));
        parent.addChild(confirm);

        ((AbstractWindowSkeleton) (Object) this).registerButton(CONFIRM_ID, () -> {
            PacketDistributor.sendToServer(new ConfirmBuildingPayload(buildingView.getPosition()));
            ((AbstractWindowSkeleton) (Object) this).close();
        });
    }

    private void legacycolonies$addHeirButton(final IBuildingView buildingView)
    {
        final View parent = buttonBuild.getParent();
        final ButtonImage heir = legacycolonies$newButton(HEIR_ID, Component.translatable("com.legacycolonies.school.button"));
        heir.setPosition(buttonBuild.getX(), freeRow(parent, buttonBuild));
        parent.addChild(heir);

        ((AbstractWindowSkeleton) (Object) this).registerButton(HEIR_ID, () -> {
            ((AbstractWindowSkeleton) (Object) this).close();
            PacketDistributor.sendToServer(new SchoolOpenPayload(buildingView.getPosition()));
        });
    }

    /**
     * A button the size of the build button.
     */
    private ButtonImage legacycolonies$newButton(final String id, final Component text)
    {
        final ButtonImage button = new ButtonImage();
        button.setID(id);
        button.setImage(ResourceLocation.fromNamespaceAndPath("minecolonies", "textures/gui/builderhut/builder_button_medium_large.png"));
        button.setSize(buttonBuild.getWidth(), buttonBuild.getHeight());
        // A button made in code starts with a zero-sized text box, which setSize only scales: give the text the whole button.
        button.setTextRenderBox(buttonBuild.getWidth(), buttonBuild.getHeight());
        button.setColors(0);
        button.setText(text);
        return button;
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
