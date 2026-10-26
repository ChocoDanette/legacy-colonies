package com.chocodanette.legacycolonies.compat;

import com.ldtteam.structurize.placement.structure.IStructureHandler;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The builder only asks for base materials (lumber, brick and mud brick blocks) and makes the blocks derived from them
 * itself: stairs, slabs, walls, plank blocks, fences, doors, trapdoors, chests and barrels. Each derived block costs
 * what its TFC crafting recipe uses, per block. A fractional cost (a stair is 3/4 of a brick block) is spread over the
 * blocks of the plan: some cost one more, some one less, by position.
 * <p>
 * Placement itself is left to the handler that would place the block otherwise; only the requested items change.
 */
public class BaseItemsPlacementHandler implements IPlacementHandler
{
    private record Cost(Pattern item, String base, double count)
    {
    }

    private static final String WOOD   = "([a-z_]+)";
    private static final String LUMBER = "tfc:wood/lumber/%s";

    /**
     * Item ids of derived blocks and their cost; the first match wins, so the plank block comes after its stairs and slabs.
     */
    private static final List<Cost> COSTS = List.of(
      cost("tfc:wood/planks/" + WOOD + "_stairs", LUMBER, 3),
      cost("tfc:wood/planks/" + WOOD + "_slab", LUMBER, 2),
      cost("tfc:wood/planks/" + WOOD, LUMBER, 4),
      cost("tfc:wood/fence/" + WOOD, LUMBER, 2.25),
      cost("tfc:wood/fence_gate/" + WOOD, LUMBER, 6),
      cost("tfc:wood/door/" + WOOD, LUMBER, 6),
      cost("tfc:wood/trapdoor/" + WOOD, LUMBER, 3),
      cost("tfc:wood/chest/" + WOOD, LUMBER, 8),
      cost("tfc:wood/barrel/" + WOOD, LUMBER, 7),
      cost("tfc:rock/bricks/" + WOOD + "_stairs", "tfc:rock/bricks/%s", 0.75),
      cost("tfc:rock/bricks/" + WOOD + "_slab", "tfc:rock/bricks/%s", 0.5),
      cost("tfc:rock/bricks/" + WOOD + "_wall", "tfc:rock/bricks/%s", 1),
      cost("tfc:mud_bricks/" + WOOD + "_stairs", "tfc:mud_bricks/%s", 0.75),
      cost("tfc:mud_bricks/" + WOOD + "_slab", "tfc:mud_bricks/%s", 0.5),
      cost("tfc:mud_bricks/" + WOOD + "_wall", "tfc:mud_bricks/%s", 1)
    );

    /**
     * The handler that places each block we take over, found the first time the block is seen.
     */
    private final Map<Block, IPlacementHandler> delegates = new ConcurrentHashMap<>();

    private static Cost cost(final String item, final String base, final double count)
    {
        return new Cost(Pattern.compile(item), base, count);
    }

    @Override
    public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState)
    {
        return costOf(blockState.getBlock().asItem()) != null && delegate(world, pos, blockState) != null;
    }

    @Nullable
    private IPlacementHandler delegate(final Level world, final BlockPos pos, final BlockState blockState)
    {
        final IPlacementHandler known = delegates.get(blockState.getBlock());
        if (known != null)
        {
            return known;
        }
        for (final IPlacementHandler handler : PlacementHandlers.handlers)
        {
            if (handler != this && handler.canHandle(world, pos, blockState))
            {
                delegates.put(blockState.getBlock(), handler);
                return handler;
            }
        }
        return null;
    }

    @Override
    public ActionProcessingResult handle(
      final Level world,
      final BlockPos pos,
      final BlockState blockState,
      @Nullable final CompoundTag tileEntityData,
      @NotNull final IPlacementContext placementContext)
    {
        final IPlacementHandler delegate = delegate(world, pos, blockState);
        return delegate == null ? ActionProcessingResult.DENY : delegate.handle(world, pos, blockState, tileEntityData, placementContext);
    }

    @Override
    public void handleRemoval(final IStructureHandler handler, final Level world, final BlockPos pos, final CompoundTag tileEntityData)
    {
        final IPlacementHandler delegate = delegate(world, pos, world.getBlockState(pos));
        if (delegate != null)
        {
            delegate.handleRemoval(handler, world, pos, tileEntityData);
        }
        else
        {
            IPlacementHandler.super.handleRemoval(handler, world, pos, tileEntityData);
        }
    }

    @Override
    public List<ItemStack> getRequiredItems(
      final Level world,
      final BlockPos pos,
      final BlockState blockState,
      @Nullable final CompoundTag tileEntityData,
      @NotNull final IPlacementContext placementContext)
    {
        final IPlacementHandler delegate = delegate(world, pos, blockState);
        if (delegate == null)
        {
            return List.of();
        }
        final List<ItemStack> items = new ArrayList<>();
        for (final ItemStack stack : delegate.getRequiredItems(world, pos, blockState, tileEntityData, placementContext))
        {
            final ItemStack base = toBase(stack, pos);
            if (!base.isEmpty())
            {
                items.add(base);
            }
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
        final IPlacementHandler delegate = delegates.get(blueprintState.getBlock());
        return delegate == null ? worldState.equals(blueprintState) : delegate.doesWorldStateMatchBlueprintState(worldState, blueprintState, blockEntityData, placementContext);
    }

    @Nullable
    private static Matcher costOf(final Item item, final Cost[] found)
    {
        final String id = BuiltInRegistries.ITEM.getKey(item).toString();
        for (final Cost cost : COSTS)
        {
            final Matcher matcher = cost.item().matcher(id);
            if (matcher.matches())
            {
                found[0] = cost;
                return matcher;
            }
        }
        return null;
    }

    @Nullable
    private static Cost costOf(final Item item)
    {
        final Cost[] found = new Cost[1];
        return costOf(item, found) == null ? null : found[0];
    }

    /**
     * The base items for a stack of derived blocks, or the stack itself when it is not a derived block.
     */
    private static ItemStack toBase(final ItemStack stack, final BlockPos pos)
    {
        final Cost[] found = new Cost[1];
        final Matcher matcher = costOf(stack.getItem(), found);
        if (matcher == null)
        {
            return stack;
        }
        final Item base = BuiltInRegistries.ITEM.get(ResourceLocation.parse(String.format(found[0].base(), matcher.group(1))));
        if (base == Items.AIR)
        {
            return stack;
        }
        final int count = amount(pos, found[0].count() * stack.getCount());
        return count <= 0 ? ItemStack.EMPTY : new ItemStack(base, count);
    }

    /**
     * The whole part of the cost, plus one for a share of the positions equal to its fractional part.
     */
    private static int amount(final BlockPos pos, final double cost)
    {
        final int whole = (int) Math.floor(cost);
        long hash = pos.asLong() * 0x9E3779B97F4A7C15L;
        hash ^= hash >>> 31;
        final double roll = (hash >>> 40) / (double) (1L << 24);
        return whole + (roll < cost - whole ? 1 : 0);
    }
}
