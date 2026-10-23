package com.chocodanette.legacycolonies.compat;

import com.chocodanette.legacycolonies.material.MaterialChoices;
import com.chocodanette.legacycolonies.material.MaterialSlot;
import com.chocodanette.legacycolonies.material.MaterialSubstitution;
import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TFC's firepit is made in the world from a log, sticks and straw. The builder requests those items and places the
 * firepit unlit. The log is of the building's wood (ash in the plans).
 */
public class FirepitPlacementHandler implements IPlacementHandler
{
    private static final ResourceLocation FIREPIT = ResourceLocation.fromNamespaceAndPath("tfc", "firepit");
    private static final ResourceLocation LOG     = ResourceLocation.fromNamespaceAndPath("tfc", "wood/log/ash");
    private static final ResourceLocation STRAW   = ResourceLocation.fromNamespaceAndPath("tfc", "straw");

    @Override
    public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState)
    {
        return BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).equals(FIREPIT);
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
        final BlockState unlit = blockState.hasProperty(BlockStateProperties.LIT) ? blockState.setValue(BlockStateProperties.LIT, false) : blockState;
        return PlacementHandlers.handleBlockPlacement(world, pos, unlit) ? ActionProcessingResult.SUCCESS : ActionProcessingResult.DENY;
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
        ItemStack log = stackOf(LOG, 1);
        // The client's resource list swaps the materials itself; the server swaps them here.
        if (world instanceof final ServerLevel level && !log.isEmpty())
        {
            final Map<MaterialSlot, ResourceLocation> chosen = MaterialChoices.get(level).getChoices(placementContext.getCenterPos());
            if (chosen != null && !chosen.isEmpty())
            {
                log = MaterialSubstitution.substitute(log, chosen);
            }
        }
        if (!log.isEmpty())
        {
            items.add(log);
        }
        items.add(new ItemStack(Items.STICK, 3));
        final ItemStack straw = stackOf(STRAW, 3);
        if (!straw.isEmpty())
        {
            items.add(straw);
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
        // Lit or not, a firepit is a firepit.
        return worldState.getBlock() == blueprintState.getBlock();
    }
}
