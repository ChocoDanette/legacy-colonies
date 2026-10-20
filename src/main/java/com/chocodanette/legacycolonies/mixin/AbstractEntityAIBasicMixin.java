package com.chocodanette.legacycolonies.mixin;

import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The builder clears blocks by hand when it has no fitting tool, instead of requesting one.
 */
@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public abstract class AbstractEntityAIBasicMixin
{
    /**
     * {@code AbstractEntityAIBasic.NO_TOOL}: work with an empty hand.
     */
    private static final int NO_TOOL = -10;

    @Inject(method = "getMostEfficientTool", at = @At("RETURN"), cancellable = true)
    private void legacycolonies$builderMinesByHand(final BlockState target, final BlockPos pos, final CallbackInfoReturnable<Integer> cir)
    {
        if (cir.getReturnValueI() < 0 && (Object) this instanceof EntityAIStructureBuilder)
        {
            cir.setReturnValue(NO_TOOL);
        }
    }
}
