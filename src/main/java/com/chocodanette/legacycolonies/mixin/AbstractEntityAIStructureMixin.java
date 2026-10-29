package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.colony.BuilderTools;
import com.chocodanette.legacycolonies.pile.MaterialPiles;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * After placing a block, the builder waits less with a tool for that block (see {@link BuilderTools}). And it only
 * starts a work order once its material pile is complete (see {@link MaterialPiles}).
 */
@Mixin(value = AbstractEntityAIStructure.class, remap = false)
public abstract class AbstractEntityAIStructureMixin
{
    @ModifyArg(method = "structureStep", at = @At(value = "INVOKE", target = "setDelay(I)V"))
    private int legacycolonies$toolSpeed(final int delay)
    {
        final BlockState placed = BuilderTools.LAST_PLACED.get();
        BuilderTools.LAST_PLACED.remove();
        if (placed == null || !((Object) this instanceof EntityAIStructureBuilder builder))
        {
            return delay;
        }
        return (int) Math.round(delay / BuilderTools.speed(builder.getWorker().getInventoryCitizen(), placed));
    }

    @Inject(method = "afterStructureLoading", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$waitForPile(final CallbackInfoReturnable<IAIState> cir)
    {
        if ((Object) this instanceof EntityAIStructureBuilder builder && !MaterialPiles.mayStart(builder))
        {
            cir.setReturnValue(AIWorkerState.LOAD_STRUCTURE);
        }
    }

    @Inject(method = "hasListOfResInInvOrRequest", at = @At("HEAD"))
    private static void legacycolonies$takeFromPile(
      final AbstractEntityAIStructure<?, ?> placer,
      final List<ItemStack> itemList,
      final boolean force,
      final CallbackInfoReturnable<?> cir)
    {
        if (placer instanceof EntityAIStructureBuilder builder)
        {
            MaterialPiles.supply(builder, itemList);
        }
    }
}
