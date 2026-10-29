package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.colony.BuilderTools;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * After placing a block, the builder waits less with a tool for that block (see {@link BuilderTools}).
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
}
