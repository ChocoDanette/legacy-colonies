package com.chocodanette.legacycolonies.compat;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TFC's thatch bed is made in the world: a large raw hide used on two thatch blocks side by side. The builder requests
 * 2 thatch and 1 large raw hide at the foot of the bed, and places both halves at once, as TFC does. The head keeps
 * the thatch and the hide, so breaking the bed gives them back.
 */
public class ThatchBedPlacementHandler implements IPlacementHandler
{
    private static final ResourceLocation THATCH_BED = ResourceLocation.fromNamespaceAndPath("tfc", "thatch_bed");
    private static final ResourceLocation THATCH     = ResourceLocation.fromNamespaceAndPath("tfc", "thatch");
    private static final ResourceLocation HIDE       = ResourceLocation.fromNamespaceAndPath("tfc", "large_raw_hide");

    /**
     * {@link Block#UPDATE_CLIENTS} and {@link Block#UPDATE_KNOWN_SHAPE}, like TFC: a lone half is not removed before the
     * other one is placed.
     */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    @Override
    public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState)
    {
        return BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).equals(THATCH_BED);
    }

    @Override
    public ActionProcessingResult handle(
      final Level world,
      final BlockPos pos,
      final BlockState blockState,
      @Nullable final CompoundTag tileEntityData,
      @NotNull final IPlacementContext placementContext)
    {
        if (blockState.getValue(BedBlock.PART) == BedPart.HEAD)
        {
            // Placed with the foot.
            return ActionProcessingResult.PASS;
        }
        final BlockPos headPos = pos.relative(blockState.getValue(BedBlock.FACING));
        final BlockState head = blockState.setValue(BedBlock.PART, BedPart.HEAD);
        if (world.getBlockState(pos).equals(blockState) && world.getBlockState(headPos).equals(head))
        {
            return ActionProcessingResult.PASS;
        }

        world.setBlock(pos, blockState, FLAGS);
        world.setBlock(headPos, head, FLAGS);
        final BlockEntity bed = world.getBlockEntity(headPos);
        final Block thatch = BuiltInRegistries.BLOCK.get(THATCH);
        if (bed != null)
        {
            // ThatchBedBlockEntity.setBed(head thatch, foot thatch, hide): TFC is not a compile dependency.
            try
            {
                bed.getClass().getMethod("setBed", BlockState.class, BlockState.class, ItemStack.class)
                  .invoke(bed, thatch.defaultBlockState(), thatch.defaultBlockState(), stackOf(HIDE, 1));
            }
            catch (final ReflectiveOperationException e)
            {
                LegacyColonies.LOGGER.warn("Could not store the materials of a thatch bed at {}", headPos, e);
            }
        }
        return ActionProcessingResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getRequiredItems(
      final Level world,
      final BlockPos pos,
      final BlockState blockState,
      @Nullable final CompoundTag tileEntityData,
      @NotNull final IPlacementContext placementContext)
    {
        final List<ItemStack> items = new ArrayList<>();
        if (blockState.getValue(BedBlock.PART) == BedPart.FOOT)
        {
            final ItemStack thatch = stackOf(THATCH, 2);
            final ItemStack hide = stackOf(HIDE, 1);
            if (!thatch.isEmpty())
            {
                items.add(thatch);
            }
            if (!hide.isEmpty())
            {
                items.add(hide);
            }
        }
        return items;
    }

    private static ItemStack stackOf(final ResourceLocation id, final int count)
    {
        final Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    @Override
    public boolean doesWorldStateMatchBlueprintState(
      final BlockState worldState,
      final BlockState blueprintState,
      @Nullable final Tuple<BlockEntity, CompoundTag> blockEntityData,
      @NotNull final IPlacementContext placementContext)
    {
        // Occupied or not, a thatch bed facing the same way is the same bed.
        return worldState.getBlock() == blueprintState.getBlock()
                 && worldState.getValue(BedBlock.PART) == blueprintState.getValue(BedBlock.PART)
                 && worldState.getValue(BedBlock.FACING) == blueprintState.getValue(BedBlock.FACING);
    }
}
