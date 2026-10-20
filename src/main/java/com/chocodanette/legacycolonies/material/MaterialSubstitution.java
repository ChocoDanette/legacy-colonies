package com.chocodanette.legacycolonies.material;

import com.chocodanette.legacycolonies.mixin.BlueprintAccessor;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;
import java.util.Map;

/**
 * Replaces the reference materials of a plan by the materials chosen for its building.
 */
public final class MaterialSubstitution
{
    private MaterialSubstitution()
    {
    }

    /**
     * Called when a work order receives its plan.
     *
     * @param blueprint the freshly loaded plan, changed in place.
     * @param level     the colony's level.
     * @param location  the position of the building the plan is for.
     */
    public static void apply(final Blueprint blueprint, final Level level, final BlockPos location)
    {
        if (blueprint == null || location == null || !(level instanceof final ServerLevel serverLevel))
        {
            return;
        }
        final Map<MaterialSlot, ResourceLocation> chosen = MaterialChoices.get(serverLevel).getChoices(location);
        if (chosen == null || chosen.isEmpty())
        {
            return;
        }

        final List<BlockState> palette = ((BlueprintAccessor) blueprint).legacycolonies$getPalette();
        boolean changed = false;
        for (int i = 0; i < palette.size(); i++)
        {
            final BlockState replacement = substitute(palette.get(i), chosen);
            if (replacement != null)
            {
                palette.set(i, replacement);
                changed = true;
            }
        }
        if (changed)
        {
            ((BlueprintAccessor) blueprint).legacycolonies$cacheReset(false);
        }
    }

    private static BlockState substitute(final BlockState state, final Map<MaterialSlot, ResourceLocation> chosen)
    {
        final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        for (final Map.Entry<MaterialSlot, ResourceLocation> entry : chosen.entrySet())
        {
            final ResourceLocation targetId = entry.getKey().substitute(blockId, entry.getValue());
            if (targetId != null && !targetId.equals(blockId))
            {
                final Block target = BuiltInRegistries.BLOCK.getOptional(targetId).orElse(null);
                return target == null ? null : copyProperties(state, target.defaultBlockState());
            }
        }
        return null;
    }

    private static BlockState copyProperties(final BlockState from, BlockState to)
    {
        for (final Property<?> property : from.getProperties())
        {
            to = copyProperty(from, to, property);
        }
        return to;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(final BlockState from, final BlockState to, final Property<T> property)
    {
        return to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }
}
