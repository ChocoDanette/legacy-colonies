package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.pile.MaterialPiles;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When the builder completes its work order, the material pile gives what is left to its stock and disappears.
 */
@Mixin(value = AbstractBuildingStructureBuilder.class, remap = false)
public abstract class AbstractBuildingStructureBuilderMixin
{
    @Inject(method = "complete", at = @At("HEAD"))
    private void legacycolonies$removePile(final ICitizenData citizen, final CallbackInfo ci)
    {
        MaterialPiles.onWorkOrderDone((AbstractBuildingStructureBuilder) (Object) this);
    }
}
