package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.colony.BuilderTools;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The builder picks its tools itself (see {@link BuilderTools}): the fastest one for the block, its hand when it has
 * none, and it only asks for a tool the block requires (a pickaxe for stone).
 */
@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public abstract class AbstractEntityAIBasicMixin
{
    @Inject(method = "holdEfficientTool", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$builderTools(final BlockState target, final BlockPos pos, final CallbackInfoReturnable<Boolean> cir)
    {
        if ((Object) this instanceof EntityAIStructureBuilder builder)
        {
            cir.setReturnValue(BuilderTools.holdTool(builder, target));
        }
    }
}
