package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.colony.BuilderTools;
import com.chocodanette.legacycolonies.compat.BaseItemsPlacementHandler;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
import com.ldtteam.structurize.util.BlockInfo;
import com.ldtteam.structurize.placement.StructurePhasePlacementResult;
import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.AbstractBlueprintIterator;
import com.ldtteam.structurize.placement.BlockPlacementResult;
import com.ldtteam.structurize.util.ChangeStorage;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import com.minecolonies.core.entity.ai.workers.util.BuildingStructureHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * Tells the builder's base items handler when Structurize lists the items of a plan rather than placing it, so that
 * it counts whole craft batches over the plan (see {@link BaseItemsPlacementHandler}).
 * <p>
 * Also keeps the builder from placing a block it needs a tool for (stone without a pickaxe), and tells it which
 * block it placed, for the speed of its tools (see {@link BuilderTools}).
 */
@Mixin(value = StructurePlacer.class, remap = false)
public abstract class StructurePlacerMixin
{
    @Shadow
    @Final
    protected IStructureHandler handler;

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
        BuilderTools.LAST_PLACED.remove();
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

    @Inject(method = "handleBlockPlacement", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$requireTool(
      final Level world,
      final BlockPos worldPos,
      final ChangeStorage storage,
      final BlockInfo blockInfo,
      final CallbackInfoReturnable<BlockPlacementResult> cir)
    {
        final EntityAIStructureBuilder builder = builderOf(handler);
        final BlockState localState = blockInfo.getState();
        if (builder == null
              || localState == null
              || world.getBlockState(worldPos).getBlock() == localState.getBlock()
              || !BuilderTools.lacksRequiredTool(builder.getWorker().getInventoryCitizen(), localState)
              || IPlacementHandler.doesWorldStateMatchBlueprintState(blockInfo, worldPos, handler))
        {
            return;
        }
        // Missing items, none listed: the builder asks for nothing more and waits for the tool it was told to request.
        BuilderTools.requestTool(builder, BuilderTools.toolFor(localState));
        cir.setReturnValue(new BlockPlacementResult(worldPos, BlockPlacementResult.Result.MISSING_ITEMS, new ArrayList<>()));
    }

    @Inject(method = "handleBlockPlacement", at = @At("RETURN"))
    private void legacycolonies$rememberPlaced(
      final Level world,
      final BlockPos worldPos,
      final ChangeStorage storage,
      final BlockInfo blockInfo,
      final CallbackInfoReturnable<BlockPlacementResult> cir)
    {
        if (cir.getReturnValue().getResult() == BlockPlacementResult.Result.SUCCESS && builderOf(handler) != null)
        {
            BuilderTools.LAST_PLACED.set(blockInfo.getState());
        }
    }

    private static EntityAIStructureBuilder builderOf(final IStructureHandler handler)
    {
        if (handler instanceof BuildingStructureHandler<?, ?> structureHandler
              && ((BuildingStructureHandlerAccessor) structureHandler).legacycolonies$getStructureAI() instanceof EntityAIStructureBuilder builder)
        {
            return builder;
        }
        return null;
    }
}
