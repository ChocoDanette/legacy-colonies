package com.chocodanette.legacycolonies.compat;

import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * TFC's charcoal forge is a pile of 7 charcoal, lit in a ring of stone. The builder requests the 7 charcoal and places the
 * forge cold; the player lights it.
 */
public class CharcoalForgePlacementHandler implements IPlacementHandler
{
    private static final ResourceLocation FORGE    = ResourceLocation.fromNamespaceAndPath("tfc", "charcoal_forge");
    private static final int              CHARCOAL = 7;

    @Override
    public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState)
    {
        return BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).equals(FORGE);
    }

    @Override
    public ActionProcessingResult handle(
      final Level world,
      final BlockPos pos,
      final BlockState blockState,
      @Nullable final CompoundTag tileEntityData,
      @NotNull final IPlacementContext placementContext)
    {
        if (world.getBlockState(pos).getBlock() == blockState.getBlock())
        {
            return ActionProcessingResult.PASS;
        }
        return PlacementHandlers.handleBlockPlacement(world, pos, blockState.getBlock().defaultBlockState()) ? ActionProcessingResult.SUCCESS : ActionProcessingResult.DENY;
    }

    @Override
    public List<ItemStack> getRequiredItems(
      final Level world,
      final BlockPos pos,
      final BlockState blockState,
      @Nullable final CompoundTag tileEntityData,
      @NotNull final IPlacementContext placementContext)
    {
        return List.of(new ItemStack(Items.CHARCOAL, CHARCOAL));
    }

    @Override
    public boolean doesWorldStateMatchBlueprintState(
      final BlockState worldState,
      final BlockState blueprintState,
      @Nullable final Tuple<BlockEntity, CompoundTag> blockEntityData,
      @NotNull final IPlacementContext placementContext)
    {
        // Hot or cold, a forge is a forge.
        return worldState.getBlock() == blueprintState.getBlock();
    }
}
