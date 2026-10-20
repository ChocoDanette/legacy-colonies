package com.chocodanette.legacycolonies.mixin.client;

import com.chocodanette.legacycolonies.material.ClientMaterialChoices;
import com.chocodanette.legacycolonies.material.MaterialSlot;
import com.chocodanette.legacycolonies.material.MaterialSubstitution;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.client.gui.WindowBuildBuilding;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

/**
 * The build window lists the resources of the plan as drawn: list the chosen materials instead.
 */
@Mixin(value = WindowBuildBuilding.class, remap = false)
public abstract class WindowBuildBuildingMixin
{
    @Shadow
    @Final
    private IBuildingView building;

    @ModifyVariable(method = "addNeededResource", at = @At("HEAD"), argsOnly = true)
    private ItemStack legacycolonies$substituteMaterials(final ItemStack stack)
    {
        final Map<MaterialSlot, ResourceLocation> chosen = ClientMaterialChoices.getChoices(building.getPosition());
        return stack == null || chosen == null || chosen.isEmpty() ? stack : MaterialSubstitution.substitute(stack, chosen);
    }
}
