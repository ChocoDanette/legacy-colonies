package com.chocodanette.legacycolonies.mixin;

import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import com.minecolonies.core.entity.ai.workers.util.BuildingStructureHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Gives the worker that places a plan, to check its tools.
 */
@Mixin(value = BuildingStructureHandler.class, remap = false)
public interface BuildingStructureHandlerAccessor
{
    @Accessor("structureAI")
    AbstractEntityAIStructure<?, ?> legacycolonies$getStructureAI();
}
