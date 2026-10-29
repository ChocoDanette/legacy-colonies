package com.chocodanette.legacycolonies.pile;

import com.chocodanette.legacycolonies.ModBlocks;
import com.chocodanette.legacycolonies.colony.BuilderTools;
import com.chocodanette.legacycolonies.mixin.AbstractEntityAIStructureAccessor;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.workorders.IBuilderWorkOrder;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import com.minecolonies.core.entity.ai.workers.util.BuildingStructureHandler;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.minecolonies.api.util.Tuple;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The material pile: when the builder starts a work order (build, upgrade, repair), a pile appears next to the site.
 * The player brings it the exact materials the builder listed; the builder starts only once the pile is complete, and
 * takes its materials from it, as from a rack of its post. The pile disappears when the work order ends: what is left
 * goes to the builder's stock when the building is done, or falls on the ground when the work order is cancelled.
 */
public final class MaterialPiles
{
    /**
     * Ticks between two checks that the work order of a pile still exists.
     */
    public static final int CHECK_INTERVAL = 100;

    /**
     * How far around the site the pile may be placed.
     */
    private static final int SEARCH_DISTANCE = 6;

    private static final String KEY = "com.legacycolonies.material_pile.";

    private MaterialPiles()
    {
    }

    /**
     * Called when the builder has listed the materials of its work order and would start: whether it may.
     */
    public static boolean mayStart(final EntityAIStructureBuilder ai)
    {
        if (!(ai.getWorker().getCitizenData().getWorkBuilding() instanceof AbstractBuildingStructureBuilder builder))
        {
            return true;
        }
        final IBuilderWorkOrder workOrder = builder.getWorkOrder();
        if (workOrder == null || workOrder.getWorkOrderType() == WorkOrderType.REMOVE)
        {
            return true;
        }
        final Level level = ai.getWorker().level();
        final MaterialPileBlockEntity pile = find(level, builder, workOrder.getID());
        if (pile == null)
        {
            final List<ItemStorage> materials = new ArrayList<>(builder.getNeededResources().values());
            if (materials.stream().noneMatch(material -> material.getAmount() > 0))
            {
                return true;
            }
            final MaterialPileBlockEntity placed = place(level, ai, builder, workOrder, materials);
            if (placed == null)
            {
                return true;
            }
            ai.getWorker().getCitizenData().setJobStatus(JobStatus.STUCK);
            final BlockPos pos = placed.getBlockPos();
            MessageUtils.format(Component.translatable(KEY + "placed", workOrder.getDisplayName(), pos.getX(), pos.getY(), pos.getZ()))
              .sendTo(builder.getColony())
              .forAllPlayers();
            return false;
        }
        if (!pile.isReady())
        {
            ai.getWorker().getCitizenData().setJobStatus(JobStatus.STUCK);
            return false;
        }
        if (!pile.isAnnounced())
        {
            pile.setAnnounced();
            emptyInventory(ai, builder);
            MessageUtils.format(Component.translatable(KEY + "complete", workOrder.getDisplayName())).sendTo(builder.getColony()).forAllPlayers();
        }
        ai.getWorker().getCitizenData().setJobStatus(JobStatus.WORKING);
        return true;
    }

    /**
     * The pile of this work order among the builder's racks.
     */
    @Nullable
    private static MaterialPileBlockEntity find(final Level level, final AbstractBuildingStructureBuilder builder, final int workOrderId)
    {
        for (final BlockPos pos : builder.getContainers())
        {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof MaterialPileBlockEntity pile && pile.getWorkOrderId() == workOrderId)
            {
                return pile;
            }
        }
        return null;
    }

    @Nullable
    private static MaterialPileBlockEntity place(
      final Level level,
      final EntityAIStructureBuilder ai,
      final AbstractBuildingStructureBuilder builder,
      final IBuilderWorkOrder workOrder,
      final List<ItemStorage> materials)
    {
        final BlockPos pos = findSpot(level, ai, workOrder.getLocation());
        if (pos == null)
        {
            return null;
        }
        level.setBlock(pos, ModBlocks.MATERIAL_PILE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof MaterialPileBlockEntity pile))
        {
            return null;
        }
        pile.setup(builder.getColony().getID(), workOrder.getID(), builder.getPosition(), materials);
        builder.addContainerPosition(pos);
        builder.markDirty();
        return pile;
    }

    /**
     * A free spot on the ground just outside the site, as close to the hut as can be; next to the hut if the plan is
     * unknown.
     */
    @Nullable
    private static BlockPos findSpot(final Level level, final EntityAIStructureBuilder ai, final BlockPos hut)
    {
        BlockPos min = hut;
        BlockPos max = hut;
        final Tuple<?, ?> placer = ((AbstractEntityAIStructureAccessor) ai).legacycolonies$getStructurePlacer();
        if (placer != null && placer.getB() instanceof BuildingStructureHandler<?, ?> handler && handler.getBluePrint() != null)
        {
            final Blueprint blueprint = handler.getBluePrint();
            min = handler.getCenterPos().subtract(blueprint.getPrimaryBlockOffset());
            max = min.offset(blueprint.getSizeX() - 1, blueprint.getSizeY() - 1, blueprint.getSizeZ() - 1);
        }
        for (int distance = 1; distance <= SEARCH_DISTANCE; distance++)
        {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int x = min.getX() - distance; x <= max.getX() + distance; x++)
            {
                for (int z = min.getZ() - distance; z <= max.getZ() + distance; z++)
                {
                    final boolean onRing = x == min.getX() - distance || x == max.getX() + distance || z == min.getZ() - distance || z == max.getZ() + distance;
                    if (!onRing)
                    {
                        continue;
                    }
                    final BlockPos candidate = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                    if (isFree(level, candidate, hut))
                    {
                        final double candidateDistance = candidate.distSqr(hut);
                        if (candidateDistance < bestDistance)
                        {
                            best = candidate;
                            bestDistance = candidateDistance;
                        }
                    }
                }
            }
            if (best != null)
            {
                return best;
            }
        }
        return null;
    }

    private static boolean isFree(final Level level, final BlockPos pos, final BlockPos hut)
    {
        return level.isLoaded(pos)
                 && Math.abs(pos.getY() - hut.getY()) <= 8
                 && level.getBlockState(pos).canBeReplaced()
                 && level.getFluidState(pos).isEmpty()
                 && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                 && level.getBlockState(pos.above()).isAir();
    }

    /**
     * Puts the stack (or, when null, every needed item of the player's inventory) in the pile, and tells the player what
     * is still missing.
     */
    public static void deposit(final ServerPlayer player, final MaterialPileBlockEntity pile, @Nullable final ItemStack stack)
    {
        int moved = 0;
        if (stack != null)
        {
            moved = pile.deposit(stack);
        }
        else
        {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            {
                final ItemStack held = player.getInventory().getItem(slot);
                if (!held.isEmpty())
                {
                    moved += pile.deposit(held);
                }
            }
        }
        if (moved > 0)
        {
            player.getInventory().setChanged();
            player.displayClientMessage(Component.translatable(KEY + "deposited", moved), true);
        }
        showMissing(player, pile);
    }

    /**
     * Tells the player what the pile still misses, or that it is complete.
     */
    public static void showMissing(final ServerPlayer player, final MaterialPileBlockEntity pile)
    {
        final List<ItemStorage> missing = pile.missing();
        if (missing.isEmpty())
        {
            player.sendSystemMessage(Component.translatable(KEY + "full"));
            return;
        }
        final MutableComponent message = Component.translatable(KEY + "missing");
        for (final ItemStorage material : missing)
        {
            message.append("\n- ").append(Component.translatable(KEY + "line", material.getAmount(), material.getItemStack().getHoverName()));
        }
        player.sendSystemMessage(message);
    }

    /**
     * When the builder is done with its work order: what is left in the pile goes to the builder's stock, and the pile
     * disappears.
     */
    public static void onWorkOrderDone(final AbstractBuildingStructureBuilder builder)
    {
        final IBuilderWorkOrder workOrder = builder.getWorkOrder();
        final Level level = builder.getColony().getWorld();
        if (workOrder == null || level == null)
        {
            return;
        }
        final MaterialPileBlockEntity pile = find(level, builder, workOrder.getID());
        if (pile == null)
        {
            return;
        }
        final AbstractTileEntityColonyBuilding hut = builder.getTileEntity();
        for (final ItemStack stack : pile.takeAll())
        {
            final ItemStack rest = hut == null ? stack : ItemHandlerHelper.insertItemStacked(hut.getInventory(), stack, false);
            if (!rest.isEmpty())
            {
                Containers.dropItemStack(level, pile.getBlockPos().getX() + 0.5, pile.getBlockPos().getY() + 0.5, pile.getBlockPos().getZ() + 0.5, rest);
            }
        }
        remove(level, pile, builder);
    }

    /**
     * Whether the pile's work order still exists. If not, its contents fall on the ground and it disappears.
     */
    public static boolean checkStillNeeded(final ServerLevel level, final MaterialPileBlockEntity pile)
    {
        final IBuilding building = IColonyManager.getInstance().getBuilding(level, pile.getBuilderPos());
        if (building instanceof AbstractBuildingStructureBuilder builder
              && builder.getColony().getID() == pile.getColonyId()
              && builder.hasWorkOrder()
              && builder.getWorkOrder().getID() == pile.getWorkOrderId())
        {
            if (pile.isReady())
            {
                // What the builder asked for that the pile holds (asked before the pile was ready, or before 0.12.0):
                // it would wait for those requests forever.
                for (final ICitizenData citizen : builder.getAllAssignedCitizen())
                {
                    for (int slot = 0; slot < pile.getInventory().getSlots(); slot++)
                    {
                        final ItemStack stored = pile.getInventory().getStackInSlot(slot);
                        if (!stored.isEmpty())
                        {
                            cancelRequests(builder, citizen.getId(), stored);
                        }
                    }
                }
            }
            return true;
        }
        if (building == null && IColonyManager.getInstance().getColonyByWorld(pile.getColonyId(), level) != null
              && !level.isLoaded(pile.getBuilderPos()))
        {
            // The builder's post is in an unloaded chunk: ask again later.
            return true;
        }
        for (final ItemStack stack : pile.takeAll())
        {
            Containers.dropItemStack(level, pile.getBlockPos().getX() + 0.5, pile.getBlockPos().getY() + 0.5, pile.getBlockPos().getZ() + 0.5, stack);
        }
        remove(level, pile, building instanceof AbstractBuildingStructureBuilder builder ? builder : null);
        return false;
    }

    private static void remove(final Level level, final MaterialPileBlockEntity pile, @Nullable final AbstractBuildingStructureBuilder builder)
    {
        final BlockPos pos = pile.getBlockPos();
        if (builder != null)
        {
            builder.removeContainerPosition(pos);
            builder.markDirty();
        }
        level.removeBlock(pos, false);
    }

    /**
     * The pile of the builder's current work order once it was complete (the builder then takes from it), if any.
     */
    @Nullable
    public static MaterialPileBlockEntity completePile(final AbstractBuildingStructureBuilder builder)
    {
        final IBuilderWorkOrder workOrder = builder.getWorkOrder();
        final Level level = builder.getColony().getWorld();
        if (workOrder == null || level == null)
        {
            return null;
        }
        final MaterialPileBlockEntity pile = find(level, builder, workOrder.getID());
        return pile != null && pile.isReady() ? pile : null;
    }

    /**
     * Before the builder asks for the items of the next block: it takes them from its pile into its inventory (up to a
     * stack), so it never asks the player for what the pile holds. Requests it made before for these items are
     * cancelled.
     */
    public static void supply(final EntityAIStructureBuilder ai, final List<ItemStack> items)
    {
        final AbstractBuildingStructureBuilder builder = builderOf(ai);
        final MaterialPileBlockEntity pile = builder == null ? null : completePile(builder);
        if (pile == null)
        {
            return;
        }
        for (final ItemStack wanted : items)
        {
            final int material = wanted.isEmpty() ? -1 : pile.indexOf(wanted);
            if (material >= 0)
            {
                syncCredit(ai, pile, material, wanted);
            }
            if (material >= 0 && pile.credit(material) < wanted.getCount())
            {
                take(ai, pile, material, wanted, Math.max(wanted.getCount(), wanted.getMaxStackSize()) - pile.credit(material));
                if (pile.credit(material) >= wanted.getCount())
                {
                    cancelRequests(builder, ai.getWorker().getCitizenData().getId(), wanted);
                }
            }
        }
    }

    /**
     * Before the builder places a block: it may only use, of the pile's materials, those it took from the pile (not
     * its own stock, nor what it dug up). It takes what it lacks; if the pile has no more, the pile asks the player
     * for the rest and the block waits.
     *
     * @return false when the block must wait.
     */
    public static boolean mayPlace(final EntityAIStructureBuilder ai, final List<ItemStack> items)
    {
        final AbstractBuildingStructureBuilder builder = builderOf(ai);
        final MaterialPileBlockEntity pile = builder == null ? null : completePile(builder);
        if (pile == null)
        {
            return true;
        }
        boolean mayPlace = true;
        for (final ItemStack wanted : items)
        {
            final int material = wanted.isEmpty() ? -1 : pile.indexOf(wanted);
            if (material >= 0)
            {
                syncCredit(ai, pile, material, wanted);
            }
            if (material < 0 || pile.credit(material) >= wanted.getCount())
            {
                continue;
            }
            take(ai, pile, material, wanted, Math.max(wanted.getCount(), wanted.getMaxStackSize()) - pile.credit(material));
            final int lacking = wanted.getCount() - pile.credit(material);
            if (lacking <= 0)
            {
                continue;
            }
            mayPlace = false;
            if (pile.stored(material) == 0)
            {
                pile.raise(material, lacking);
                final BlockPos pos = pile.getBlockPos();
                MessageUtils.format(Component.translatable(KEY + "more", lacking, wanted.getHoverName(), pos.getX(), pos.getY(), pos.getZ()))
                  .sendTo(builder.getColony())
                  .forAllPlayers();
            }
        }
        return mayPlace;
    }

    /**
     * When the builder used items to place a block: the pile's materials among them are no longer owed to it.
     */
    public static void onConsume(final EntityAIStructureBuilder ai, final List<ItemStack> items)
    {
        final AbstractBuildingStructureBuilder builder = builderOf(ai);
        final MaterialPileBlockEntity pile = builder == null ? null : completePile(builder);
        if (pile == null)
        {
            return;
        }
        for (final ItemStack used : items)
        {
            final int material = used.isEmpty() ? -1 : pile.indexOf(used);
            if (material >= 0)
            {
                pile.addCredit(material, -used.getCount());
            }
        }
    }

    /**
     * What the builder took from the pile and no longer carries (put away in its stock) is no longer owed to it.
     */
    private static void syncCredit(final EntityAIStructureBuilder ai, final MaterialPileBlockEntity pile, final int material, final ItemStack like)
    {
        final int carried = count(ai.getWorker().getInventoryCitizen(), like);
        if (pile.credit(material) > carried)
        {
            pile.addCredit(material, carried - pile.credit(material));
        }
    }

    /**
     * Moves up to {@code amount} of a material from the pile to the builder's inventory, owed to it.
     */
    private static void take(final EntityAIStructureBuilder ai, final MaterialPileBlockEntity pile, final int material, final ItemStack like, final int amount)
    {
        final IItemHandler worker = ai.getWorker().getInventoryCitizen();
        final IItemHandler stock = pile.getInventory();
        int left = amount;
        for (int slot = 0; slot < stock.getSlots() && left > 0; slot++)
        {
            if (!ItemStackUtils.compareItemStacksIgnoreStackSize(like, stock.getStackInSlot(slot), true, true))
            {
                continue;
            }
            final ItemStack simulated = stock.extractItem(slot, left, true);
            final int fits = simulated.getCount() - ItemHandlerHelper.insertItemStacked(worker, simulated, true).getCount();
            if (fits <= 0)
            {
                break;
            }
            ItemHandlerHelper.insertItemStacked(worker, stock.extractItem(slot, fits, false), false);
            pile.addCredit(material, fits);
            left -= fits;
        }
    }

    /**
     * When the builder starts from its pile, everything it carries but its tools goes to its stock: it builds with the
     * pile's materials only.
     */
    private static void emptyInventory(final EntityAIStructureBuilder ai, final AbstractBuildingStructureBuilder builder)
    {
        final AbstractTileEntityColonyBuilding hut = builder.getTileEntity();
        if (hut == null)
        {
            return;
        }
        final IItemHandler worker = ai.getWorker().getInventoryCitizen();
        for (int slot = 0; slot < worker.getSlots(); slot++)
        {
            final ItemStack stack = worker.getStackInSlot(slot);
            if (stack.isEmpty() || BuilderTools.isTool(stack))
            {
                continue;
            }
            final ItemStack simulated = worker.extractItem(slot, stack.getCount(), true);
            final int fits = simulated.getCount() - ItemHandlerHelper.insertItemStacked(hut.getInventory(), simulated, true).getCount();
            if (fits > 0)
            {
                ItemHandlerHelper.insertItemStacked(hut.getInventory(), worker.extractItem(slot, fits, false), false);
            }
        }
    }

    @Nullable
    private static AbstractBuildingStructureBuilder builderOf(final EntityAIStructureBuilder ai)
    {
        return ai.getWorker().getCitizenData().getWorkBuilding() instanceof AbstractBuildingStructureBuilder builder ? builder : null;
    }

    private static int count(final IItemHandler handler, final ItemStack like)
    {
        int count = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (ItemStackUtils.compareItemStacksIgnoreStackSize(like, stack, true, true))
            {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void cancelRequests(final AbstractBuildingStructureBuilder builder, final int citizenId, final ItemStack stack)
    {
        final List<IRequest<?>> requests = new ArrayList<>(builder.getOpenRequestsOfCitizenOrBuilding(
          citizenId,
          request -> request.getRequest() instanceof IDeliverable deliverable && deliverable.matches(stack)));
        for (final IRequest<?> request : requests)
        {
            try
            {
                builder.getColony().getRequestManager().updateRequestState(request.getId(), RequestState.CANCELLED);
            }
            catch (final IllegalArgumentException e)
            {
                // Already gone.
            }
        }
    }
}
