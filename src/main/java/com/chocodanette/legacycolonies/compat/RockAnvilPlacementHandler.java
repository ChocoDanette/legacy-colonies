package com.chocodanette.legacycolonies.compat;

import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A TFC stone anvil ({@code tfc:rock/anvil/<rock>}) is raw rock struck with a hammer. The builder requests a raw block
 * of that rock and places the anvil.
 */
public class RockAnvilPlacementHandler implements IPlacementHandler
{
    private static final String TFC    = "tfc";
    private static final String PREFIX = "rock/anvil/";

    @Override
    public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState)
    {
        final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        return id.getNamespace().equals(TFC) && id.getPath().startsWith(PREFIX);
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
        return PlacementHandlers.handleBlockPlacement(world, pos, blockState) ? ActionProcessingResult.SUCCESS : ActionProcessingResult.DENY;
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
        final String rock = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getPath().substring(PREFIX.length());
        final Item raw = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(TFC, "rock/raw/" + rock));
        if (raw != Items.AIR)
        {
            items.add(new ItemStack(raw));
        }
        return items;
    }

    @Override
    public boolean doesWorldStateMatchBlueprintState(
      final BlockState worldState,
      final BlockState blueprintState,
      @Nullable final Tuple<BlockEntity, CompoundTag> blockEntityData,
      @NotNull final IPlacementContext placementContext)
    {
        // The anvil's contents are the player's work, not part of the plan.
        return worldState.getBlock() == blueprintState.getBlock();
    }
}
