package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.compat.BaseItemsPlacementHandler;
import com.ldtteam.structurize.placement.StructurePhasePlacementResult;
import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.AbstractBlueprintIterator;
import com.ldtteam.structurize.placement.BlockPlacementResult;
import com.ldtteam.structurize.util.ChangeStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

/**
 * Tells the builder's base items handler when Structurize lists the items of a plan rather than placing it, so that
 * it counts whole craft batches over the plan (see {@link BaseItemsPlacementHandler}).
 */
@Mixin(value = StructurePlacer.class, remap = false)
public abstract class StructurePlacerMixin
{
    @Inject(method = "executeStructureStep", at = @At("HEAD"))
    private void legacycolonies$startStep(
      final Level world,
      final ChangeStorage storage,
      final BlockPos inputPos,
      final StructurePlacer.Operation operation,
      final Supplier<AbstractBlueprintIterator.Result> iterateFunction,
      final boolean includeEntities,
      final CallbackInfoReturnable<StructurePhasePlacementResult> cir)
    {
        BaseItemsPlacementHandler.LISTING.set(false);
        if (operation == StructurePlacer.Operation.GET_RES_REQUIREMENTS)
        {
            BaseItemsPlacementHandler.LISTED_LEFTOVERS.get().clear();
        }
    }

    @Inject(method = "getResourceRequirements", at = @At("HEAD"))
    private void legacycolonies$startListing(
      final Level world,
      final BlockPos worldPos,
      final BlockPos localPos,
      final BlockState localState,
      final CompoundTag tileEntityData,
      final CallbackInfoReturnable<BlockPlacementResult> cir)
    {
        BaseItemsPlacementHandler.LISTING.set(true);
    }

    @Inject(method = "getResourceRequirements", at = @At("RETURN"))
    private void legacycolonies$endListing(
      final Level world,
      final BlockPos worldPos,
      final BlockPos localPos,
      final BlockState localState,
      final CompoundTag tileEntityData,
      final CallbackInfoReturnable<BlockPlacementResult> cir)
    {
        BaseItemsPlacementHandler.LISTING.set(false);
    }
}
