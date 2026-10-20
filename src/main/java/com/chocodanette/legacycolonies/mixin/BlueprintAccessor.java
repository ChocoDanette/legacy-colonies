package com.chocodanette.legacycolonies.mixin;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Gives access to the palette of a plan, to swap its materials.
 */
@Mixin(value = Blueprint.class, remap = false)
public interface BlueprintAccessor
{
    @Accessor("palette")
    List<BlockState> legacycolonies$getPalette();

    @Invoker("cacheReset")
    void legacycolonies$cacheReset(boolean resetPrimaryOffset);
}
