package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.colony.BlueprintPathRepair;
import com.chocodanette.legacycolonies.colony.BuildMode;
import com.chocodanette.legacycolonies.colony.BuildModes;
import com.chocodanette.legacycolonies.colony.LegacyBlueprints;
import com.chocodanette.legacycolonies.needs.ColonyNeeds;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The builder would rebuild a hut built by hand from its plan, over the player's design: refuse to build, upgrade or
 * repair it. Removal stays possible. The builder also refuses a level, or a new hut, that the colony's era does not allow.
 * A Legacy hut without a plan keeps its corners instead of loading "null1.blueprint" (see {@link BlueprintPathRepair}).
 * Before a work order, a Legacy hut gets its plan in the Legacy pack: MineColonies fails on a hut without a plan path
 * (StringIndexOutOfBoundsException in WorkOrderBuilding.create) and looks in the colony's style for one placed in it.
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

    @Inject(method = "requestWorkOrder", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$refuseAboveEra(final WorkOrderType type, final BlockPos builder, final CallbackInfo ci)
    {
        if (type != WorkOrderType.BUILD && type != WorkOrderType.UPGRADE)
        {
            return;
        }
        final IBuilding building = (IBuilding) (Object) this;
        final Component refusal = ColonyNeeds.refuseLevel(building, building.getBuildingLevel() + 1);
        if (refusal != null)
        {
            MessageUtils.format(refusal).sendTo(building.getColony()).forAllPlayers();
            ci.cancel();
        }
    }

    @Inject(method = "requestWorkOrder", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$preparePlan(final WorkOrderType type, final BlockPos builder, final CallbackInfo ci)
    {
        if (type == WorkOrderType.REMOVE)
        {
            return;
        }
        final IBuilding building = (IBuilding) (Object) this;
        final int target = type == WorkOrderType.REPAIR ? Math.max(1, building.getBuildingLevel()) : building.getBuildingLevel() + 1;
        final Component refusal = LegacyBlueprints.prepareForBuilder(building, target);
        if (refusal != null)
        {
            MessageUtils.format(refusal).sendTo(building.getColony()).forAllPlayers();
            ci.cancel();
        }
    }

    @Inject(method = "calculateCorners", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$keepCornersWithoutPlan(final CallbackInfo ci)
    {
        // With block data in its tile entity, MineColonies takes the corners from there and loads no plan.
        final AbstractTileEntityColonyBuilding hut = ((AbstractBuilding) (Object) this).getTileEntity();
        if ((hut == null || hut.getSchematicName().isEmpty()) && BlueprintPathRepair.hasNoPlan((IBuilding) (Object) this))
        {
            ci.cancel();
        }
    }
}
