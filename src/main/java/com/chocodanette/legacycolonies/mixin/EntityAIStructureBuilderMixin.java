package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.colony.BuilderTools;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The builder breaks a block as fast as a bare hand, times the speed of its tool (see {@link BuilderTools}).
 */
@Mixin(value = EntityAIStructureBuilder.class, remap = false)
public abstract class EntityAIStructureBuilderMixin
{
    @Inject(method = "getBlockMiningTime", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$toolSpeed(final BlockState state, final BlockPos pos, final CallbackInfoReturnable<Integer> cir)
    {
        cir.setReturnValue(BuilderTools.miningTime((EntityAIStructureBuilder) (Object) this, state, pos));
    }
}
