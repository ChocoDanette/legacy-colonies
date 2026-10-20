package com.chocodanette.legacycolonies.compat;

import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
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
 * TFC's daubed wattle ({@code tfc:wattle/unstained} and its dyed variants) has no item: it is a wattle with daub, and dye
 * for the stained ones. The builder requests those items and places the finished block.
 */
public class DaubedWattlePlacementHandler implements IPlacementHandler
{
    private static final String           TFC    = "tfc";
    private static final String           PREFIX = "wattle/";
    private static final ResourceLocation WATTLE = ResourceLocation.fromNamespaceAndPath(TFC, "wattle");
    private static final ResourceLocation DAUB   = ResourceLocation.fromNamespaceAndPath(TFC, "daub");

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
        if (world.getBlockState(pos).equals(blockState))
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
        addItem(items, WATTLE);
        addItem(items, DAUB);

        final String color = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getPath().substring(PREFIX.length());
        final DyeColor dye = DyeColor.byName(color, null);
        if (dye != null)
        {
            items.add(new ItemStack(DyeItem.byColor(dye)));
        }
        return items;
    }

    private static void addItem(final List<ItemStack> items, final ResourceLocation id)
    {
        final Item item = BuiltInRegistries.ITEM.get(id);
        if (item != Items.AIR)
        {
            items.add(new ItemStack(item));
        }
    }

    @Override
    public boolean doesWorldStateMatchBlueprintState(
      final BlockState worldState,
      final BlockState blueprintState,
      @Nullable final Tuple<BlockEntity, CompoundTag> blockEntityData,
      @NotNull final IPlacementContext placementContext)
    {
        return worldState.equals(blueprintState);
    }
}
