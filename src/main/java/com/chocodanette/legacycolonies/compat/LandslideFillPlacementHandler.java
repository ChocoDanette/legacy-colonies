package com.chocodanette.legacycolonies.compat;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

/**
 * The builder fills solid placeholders with the colony's fill block, TFC dirt. Dirt at the edge of a drop slides away
 * (TFC landslides) once the builder has moved on, leaving a hole in the foundation. Here the builder keeps filling:
 * while the block it placed would slide, the slide is played out at once (the block comes to rest lower down) and the
 * placeholder is filled again with a block from its stock, until the fill stays in place.
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
    /**
     * {@code TFCFallingBlockEntity.canFallThrough(BlockGetter, BlockPos, Direction, BlockState)}.
     */
    @Nullable
    private static final Method CAN_FALL_THROUGH;

    static
    {
        Method canLandslide = null;
        Method getLandslidePos = null;
        Method canFallThrough = null;
        try
        {
            final Class<?> recipe = Class.forName("net.dries007.tfc.common.recipes.LandslideRecipe");
            canLandslide = recipe.getMethod("canLandslide", BlockState.class);
            getLandslidePos = recipe.getMethod("getLandslidePos", Level.class, BlockPos.class, BlockState.class);
            canFallThrough = Class.forName("net.dries007.tfc.common.entities.misc.TFCFallingBlockEntity")
              .getMethod("canFallThrough", BlockGetter.class, BlockPos.class, Direction.class, BlockState.class);
        }
        catch (final ReflectiveOperationException e)
        {
            LegacyColonies.LOGGER.info("TFC landslides not found: the builder's fill is placed once");
        }
        CAN_LANDSLIDE = canLandslide;
        GET_LANDSLIDE_POS = getLandslidePos;
        CAN_FALL_THROUGH = canFallThrough;
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
        if (result != ActionProcessingResult.SUCCESS || !(world instanceof final ServerLevel level) || GET_LANDSLIDE_POS == null || CAN_FALL_THROUGH == null)
        {
            return result;
        }

        final BlockState fill = level.getBlockState(pos);
        if (!canLandslide(fill) || !(placementContext instanceof final IStructureHandler builder))
        {
            return result;
        }
        for (int i = 0; i < MAX_FILLS && landslidePos(level, pos, fill) != null; i++)
        {
            // Each new fill comes from the builder's stock; without one, TFC lets the last fill slide later.
            final List<ItemStack> items = getRequiredItems(world, pos, blockState, tileEntityData, placementContext);
            // The builder takes the first fill only once this placement returns: it must still be in the stock.
            final List<ItemStack> withFirst = items.stream().map(stack -> stack.copyWithCount(stack.getCount() * 2)).toList();
            if (!builder.isCreative() && !builder.hasRequiredItems(withFirst))
            {
                break;
            }
            if (!slide(level, pos, fill))
            {
                break;
            }
            if (!builder.isCreative())
            {
                builder.consume(items);
            }
            level.setBlock(pos, fill, Block.UPDATE_ALL);
        }
        return result;
    }

    /**
     * Plays out the slide of the fill at {@code pos}, as TFC would a moment later. TFC gives the position the block
     * falls from: the block itself when nothing holds it from below, or a free side it slides to first.
     *
     * @return true when the fill moved and {@code pos} is empty again.
     */
    private static boolean slide(final ServerLevel level, final BlockPos pos, final BlockState fill)
    {
        BlockPos current = pos;
        for (int step = 0; step < MAX_SLIDE_STEPS; step++)
        {
            final BlockPos from = landslidePos(level, current, fill);
            if (from == null)
            {
                break;
            }
            BlockPos landing = from;
            while (landing.getY() > level.getMinBuildHeight() && canFallThrough(level, landing.below(), fill))
            {
                landing = landing.below();
            }
            if (landing.equals(current))
            {
                break;
            }
            level.removeBlock(current, false);
            level.setBlock(landing, fill, Block.UPDATE_ALL);
            current = landing;
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

    private static boolean canFallThrough(final Level level, final BlockPos pos, final BlockState state)
    {
        try
        {
            return (boolean) CAN_FALL_THROUGH.invoke(null, level, pos, Direction.DOWN, state);
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
