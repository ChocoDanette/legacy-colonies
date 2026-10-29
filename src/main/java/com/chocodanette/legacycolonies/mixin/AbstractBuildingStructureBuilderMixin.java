package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.pile.MaterialPiles;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.buildings.utils.BuilderBucket;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When the builder completes its work order, the material pile gives what is left to its stock and disappears. While
 * the pile is complete, the builder asks for nothing: it takes from the pile.
 */
@Mixin(value = AbstractBuildingStructureBuilder.class, remap = false)
public abstract class AbstractBuildingStructureBuilderMixin
{
    @Inject(method = "complete", at = @At("HEAD"))
    private void legacycolonies$removePile(final ICitizenData citizen, final CallbackInfo ci)
    {
        MaterialPiles.onWorkOrderDone((AbstractBuildingStructureBuilder) (Object) this);
    }

    @Inject(method = "checkOrRequestBucket", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$noRequestsWithPile(final BuilderBucket requiredResources, final ICitizenData worker, final CallbackInfo ci)
    {
        if (MaterialPiles.completePile((AbstractBuildingStructureBuilder) (Object) this) != null)
        {
            ci.cancel();
        }
    }
}
