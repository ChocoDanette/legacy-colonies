package com.chocodanette.legacycolonies.mixin;

import com.minecolonies.api.util.Tuple;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Gives the plan a worker is building, to place the material pile next to it.
 */
@Mixin(value = AbstractEntityAIStructure.class, remap = false)
public interface AbstractEntityAIStructureAccessor
{
    @Accessor("structurePlacer")
    Tuple<?, ?> legacycolonies$getStructurePlacer();
}
