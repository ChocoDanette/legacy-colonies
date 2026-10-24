package com.chocodanette.legacycolonies.compat;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * The builder fills solid placeholders with the colony's fill block, TFC dirt. Dirt at the edge of a drop slides away
 * (TFC landslides) once the builder has moved on, leaving a hole in the foundation. Here the builder keeps filling:
 * while the block it placed would slide, the slide is played out at once (the block comes to rest lower down) and the
 * placeholder is filled again, until the fill stays in place.
 */
public class LandslideFillPlacementHandler extends PlacementHandlers.SolidSubstitutionPlacementHandler
{
    /**
     * Fills tried at one placeholder before leaving the last one to TFC.
     */
    private static final int MAX_FILLS = 16;

    /**
     * Steps of one slide, down or aside, before it is taken as ended.
     */
    private static final int MAX_SLIDE_STEPS = 64;

    /**
     * {@code LandslideRecipe.canLandslide(BlockState)} and {@code LandslideRecipe.getLandslidePos(Level, BlockPos, BlockState)}:
     * TFC is not a compile dependency. Null when TFC is absent or changed.
     */
    @Nullable
    private static final Method CAN_LANDSLIDE;
    @Nullable
    private static final Method GET_LANDSLIDE_POS;

    static
    {
        Method canLandslide = null;
        Method getLandslidePos = null;
        try
        {
            final Class<?> recipe = Class.forName("net.dries007.tfc.common.recipes.LandslideRecipe");
            canLandslide = recipe.getMethod("canLandslide", BlockState.class);
            getLandslidePos = recipe.getMethod("getLandslidePos", Level.class, BlockPos.class, BlockState.class);
        }
        catch (final ReflectiveOperationException e)
        {
            LegacyColonies.LOGGER.info("TFC landslides not found: the builder's fill is placed once");
        }
        CAN_LANDSLIDE = canLandslide;
        GET_LANDSLIDE_POS = getLandslidePos;
    }

    @Override
    public ActionProcessingResult handle(
      final Level world,
      final BlockPos pos,
      final BlockState blockState,
      @Nullable final CompoundTag tileEntityData,
      @NotNull final IPlacementContext placementContext)
    {
        final ActionProcessingResult result = super.handle(world, pos, blockState, tileEntityData, placementContext);
        if (result != ActionProcessingResult.SUCCESS || !(world instanceof final ServerLevel level) || GET_LANDSLIDE_POS == null)
        {
            return result;
        }

        final BlockState fill = level.getBlockState(pos);
        if (!canLandslide(fill))
        {
            return result;
        }
        for (int i = 0; i < MAX_FILLS && slide(level, pos, fill); i++)
        {
            level.setBlock(pos, fill, Block.UPDATE_ALL);
        }
        return result;
    }

    /**
     * Plays out the slide of the fill at {@code pos}, as TFC would a moment later.
     *
     * @return false when the fill stays in place; true when it slid away and {@code pos} is empty again.
     */
    private static boolean slide(final ServerLevel level, final BlockPos pos, final BlockState fill)
    {
        BlockPos current = pos;
        for (int step = 0; step < MAX_SLIDE_STEPS; step++)
        {
            final BlockPos next = landslidePos(level, current, fill);
            if (next == null)
            {
                break;
            }
            level.removeBlock(current, false);
            level.setBlock(next, fill, Block.UPDATE_ALL);
            current = next;
        }
        return !current.equals(pos);
    }

    private static boolean canLandslide(final BlockState state)
    {
        try
        {
            return CAN_LANDSLIDE != null && (boolean) CAN_LANDSLIDE.invoke(null, state);
        }
        catch (final ReflectiveOperationException e)
        {
            return false;
        }
    }

    @Nullable
    private static BlockPos landslidePos(final Level level, final BlockPos pos, final BlockState state)
    {
        try
        {
            return (BlockPos) GET_LANDSLIDE_POS.invoke(null, level, pos, state);
        }
        catch (final ReflectiveOperationException e)
        {
            return null;
        }
    }
}
