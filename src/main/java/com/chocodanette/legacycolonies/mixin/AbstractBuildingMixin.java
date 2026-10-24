package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.colony.BuildMode;
import com.chocodanette.legacycolonies.colony.BuildModes;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The builder would rebuild a hut built by hand from its plan, over the player's design: refuse to build, upgrade or
 * repair it. Removal stays possible.
 */
@Mixin(value = AbstractBuilding.class, remap = false)
public abstract class AbstractBuildingMixin
{
    @Inject(method = "requestWorkOrder", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$refuseHandBuilt(final WorkOrderType type, final BlockPos builder, final CallbackInfo ci)
    {
        final IBuilding building = (IBuilding) (Object) this;
        if (type != WorkOrderType.REMOVE
              && building.getColony().getWorld() instanceof final ServerLevel level
              && BuildModes.get(level).getMode(building.getPosition()) == BuildMode.HAND)
        {
            MessageUtils.format("com.legacycolonies.mode.hand_built").sendTo(building.getColony()).forAllPlayers();
            ci.cancel();
        }
    }
}
