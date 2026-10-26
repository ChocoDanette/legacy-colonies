package com.chocodanette.legacycolonies.compat;

import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The builder only asks for base materials (lumber, brick and mud brick blocks) and crafts the blocks derived from them
 * itself, with the player's TFC recipes: stairs, slabs, walls, plank blocks, fences, doors, trapdoors, chests and
 * barrels. A craft makes a whole batch (8 stairs from 6 brick blocks): the builder places one and keeps the others in
 * its inventory for the next blocks of that kind.
 * <p>
 * Placement itself is left to the handler that would place the block otherwise; only the requested items change.
 */
public class BaseItemsPlacementHandler implements IPlacementHandler
{
    /**
     * @param item   item id of the derived block; group 1 is the wood, rock or soil.
     * @param base   item id of the base material, with {@code %s} for the group.
     * @param count  base items one craft uses (intermediate plank blocks counted as 4 lumber each).
     * @param output derived items one craft makes.
     */
    private record Recipe(Pattern item, String base, int count, int output)
    {
    }

    private static final String WOOD   = "([a-z_]+)";
    private static final String LUMBER = "tfc:wood/lumber/%s";

    /**
     * The first match wins, so the plank block comes after its stairs and slabs.
     */
    private static final List<Recipe> RECIPES = List.of(
      recipe("tfc:wood/planks/" + WOOD + "_stairs", LUMBER, 24, 8),
      recipe("tfc:wood/planks/" + WOOD + "_slab", LUMBER, 12, 6),
      recipe("tfc:wood/planks/" + WOOD, LUMBER, 4, 1),
      recipe("tfc:wood/fence/" + WOOD, LUMBER, 18, 8),
      recipe("tfc:wood/fence_gate/" + WOOD, LUMBER, 12, 2),
      recipe("tfc:wood/door/" + WOOD, LUMBER, 6, 1),
      recipe("tfc:wood/trapdoor/" + WOOD, LUMBER, 6, 2),
      recipe("tfc:wood/chest/" + WOOD, LUMBER, 8, 1),
      recipe("tfc:wood/barrel/" + WOOD, LUMBER, 7, 1),
      recipe("tfc:rock/bricks/" + WOOD + "_stairs", "tfc:rock/bricks/%s", 6, 8),
      recipe("tfc:rock/bricks/" + WOOD + "_slab", "tfc:rock/bricks/%s", 3, 6),
      recipe("tfc:rock/bricks/" + WOOD + "_wall", "tfc:rock/bricks/%s", 6, 6),
      recipe("tfc:mud_bricks/" + WOOD + "_stairs", "tfc:mud_bricks/%s", 6, 8),
      recipe("tfc:mud_bricks/" + WOOD + "_slab", "tfc:mud_bricks/%s", 3, 6),
      recipe("tfc:mud_bricks/" + WOOD + "_wall", "tfc:mud_bricks/%s", 6, 6)
    );

    /**
     * True while Structurize lists the items of a plan (see {@code StructurePlacerMixin}), not placing it.
     */
    public static final ThreadLocal<Boolean> LISTING = ThreadLocal.withInitial(() -> false);

    /**
     * While listing, the derived items left over from the batches counted so far, per item.
     */
    public static final ThreadLocal<Map<Item, Integer>> LISTED_LEFTOVERS = ThreadLocal.withInitial(HashMap::new);

    private record Craft(Item derived, ItemStack inputs, int output)
    {
    }

    /**
     * The handler that places each block we take over, found the first time the block is seen.
     */
    private final Map<Block, IPlacementHandler> delegates = new ConcurrentHashMap<>();

    private static Recipe recipe(final String item, final String base, final int count, final int output)
    {
        return new Recipe(Pattern.compile(item), base, count, output);
    }

    @Override
    public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState)
    {
        return craftOf(blockState.getBlock().asItem()) != null && delegate(world, pos, blockState) != null;
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
        if (delegate == null)
        {
            return ActionProcessingResult.DENY;
        }
        final IItemHandler inventory = inventoryOf(placementContext);
        final Craft craft = craftOf(blockState.getBlock().asItem());
        // Decided like getRequiredItems, before placing: without one in stock, this block comes from a fresh batch.
        final boolean fromBatch = inventory != null && craft != null && count(inventory, craft.derived()) == 0;
        final ActionProcessingResult result = delegate.handle(world, pos, blockState, tileEntityData, placementContext);
        if (fromBatch && result == ActionProcessingResult.SUCCESS && craft.output() > 1)
        {
            final ItemStack rest = ItemHandlerHelper.insertItemStacked(inventory, new ItemStack(craft.derived(), craft.output() - 1), false);
            if (!rest.isEmpty())
            {
                Block.popResource(world, pos, rest);
            }
        }
        return result;
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
        final IItemHandler inventory = inventoryOf(placementContext);
        for (final ItemStack stack : delegate.getRequiredItems(world, pos, blockState, tileEntityData, placementContext))
        {
            final Craft craft = craftOf(stack.getItem());
            if (craft == null)
            {
                items.add(stack);
            }
            else if (LISTING.get())
            {
                // Listing a plan: count whole batches, using up the leftovers of earlier ones (and the builder's stock).
                final Map<Item, Integer> leftovers = LISTED_LEFTOVERS.get();
                final int stock = leftovers.computeIfAbsent(craft.derived(), item -> inventory == null ? 0 : count(inventory, item));
                if (stock > 0)
                {
                    leftovers.put(craft.derived(), stock - 1);
                }
                else
                {
                    items.add(craft.inputs().copy());
                    leftovers.put(craft.derived(), craft.output() - 1);
                }
            }
            else if (inventory != null && count(inventory, craft.derived()) > 0)
            {
                // One left from an earlier batch.
                items.add(stack);
            }
            else
            {
                items.add(craft.inputs().copy());
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

    /**
     * The craft that makes this item, or null when the builder asks for the item itself.
     */
    @Nullable
    private static Craft craftOf(final Item item)
    {
        final String id = BuiltInRegistries.ITEM.getKey(item).toString();
        for (final Recipe recipe : RECIPES)
        {
            final Matcher matcher = recipe.item().matcher(id);
            if (matcher.matches())
            {
                final Item base = BuiltInRegistries.ITEM.get(ResourceLocation.parse(String.format(recipe.base(), matcher.group(1))));
                return base == Items.AIR ? null : new Craft(item, new ItemStack(base, recipe.count()), recipe.output());
            }
        }
        return null;
    }

    @Nullable
    private static IItemHandler inventoryOf(final IPlacementContext placementContext)
    {
        return placementContext instanceof final IStructureHandler handler && !handler.isCreative() ? handler.getInventory() : null;
    }

    private static int count(final IItemHandler inventory, final Item item)
    {
        int count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++)
        {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(item))
            {
                count += stack.getCount();
            }
        }
        return count;
    }
}
