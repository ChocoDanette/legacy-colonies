package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.colony.BuildModeHandler;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.workorders.IWorkOrder;
import com.minecolonies.core.colony.workorders.WorkOrderBuilding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A new building work order means the player gave the hut to the builder.
 */
@Mixin(value = WorkOrderBuilding.class, remap = false)
public abstract class WorkOrderBuildingMixin
{
    @Inject(method = "onAdded", at = @At("TAIL"))
    private void legacycolonies$chooseBuilder(final IColony colony, final boolean readingFromNbt, final CallbackInfo ci)
    {
        if (!readingFromNbt)
        {
            BuildModeHandler.onWorkOrderAdded(colony, (IWorkOrder) (Object) this);
        }
    }
}
