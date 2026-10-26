package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.colony.BlueprintPathRepair;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.buildings.AbstractSchematicProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A Legacy hut without a plan has nothing to load for its prestige or its block data: skip it instead of looking for
 * "null1.blueprint" (see {@link BlueprintPathRepair}).
 */
@Mixin(value = AbstractSchematicProvider.class, remap = false)
public abstract class AbstractSchematicProviderMixin
{
    @Inject(method = "asyncPrestigeRecalc", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$skipPrestigeWithoutPlan(final CallbackInfo ci)
    {
        final IBuilding building = (IBuilding) (Object) this;
        if (BlueprintPathRepair.hasNoPlan(building))
        {
            building.getColony().getServerBuildingManager().clearPendingPrestigeCalc(building);
            ci.cancel();
        }
    }

    @Inject(method = "safeUpdateTEDataFromSchematic", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$skipTEDataWithoutPlan(final CallbackInfo ci)
    {
        if (BlueprintPathRepair.hasNoPlan((IBuilding) (Object) this))
        {
            ci.cancel();
        }
    }
}
