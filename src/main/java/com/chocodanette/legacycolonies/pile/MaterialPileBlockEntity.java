package com.chocodanette.legacycolonies.pile;

import com.chocodanette.legacycolonies.ModBlocks;
import com.chocodanette.legacycolonies.block.BlockMaterialPile;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.tileentities.AbstractTileEntityRack;
import com.minecolonies.api.util.ItemStackUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The material pile of one work order of the builder: a rack of its builder's post that only takes, from the player,
 * the materials the work order needs, up to the amount it needs (see {@link MaterialPiles}). Workers may take from it
 * but never put anything in it.
 */
public class MaterialPileBlockEntity extends AbstractTileEntityRack
{
    private static final String TAG_COLONY     = "colony";
    private static final String TAG_WORK_ORDER = "work_order";
    private static final String TAG_REQUIRED   = "required";
    private static final String TAG_ITEM       = "item";
    private static final String TAG_COUNT      = "count";
    private static final String TAG_SIZE       = "size";
    private static final String TAG_INVENTORY  = "inventory";
    private static final String TAG_ANNOUNCED  = "announced";
    private static final String TAG_STARTED    = "started";

    private int     colonyId;
    private int     workOrderId;
    private boolean announced;
    private boolean started;

    /**
     * The materials of the work order, one count per item, in the order the builder lists them.
     */
    private final List<ItemStorage> required = new ArrayList<>();

    /**
     * True while the player puts materials in: any other insertion is refused.
     */
    private boolean depositing;

    public MaterialPileBlockEntity(final BlockPos pos, final BlockState state)
    {
        super(ModBlocks.MATERIAL_PILE_ENTITY.get(), pos, state, 0);
    }

    @Override
    public ItemStackHandler createInventory(final int size)
    {
        return new PileInventory(size);
    }

    /**
     * Ties the pile to a work order and the materials it needs.
     */
    public void setup(final int colony, final int workOrder, final BlockPos builderPos, final List<ItemStorage> materials)
    {
        colonyId = colony;
        workOrderId = workOrder;
        buildingPos = builderPos;
        required.clear();
        int slots = 0;
        for (final ItemStorage material : materials)
        {
            if (material.getAmount() > 0 && !material.getItemStack().isEmpty())
            {
                required.add(new ItemStorage(material.getItemStack().copyWithCount(1), material.getAmount()));
                slots += (material.getAmount() + material.getItemStack().getMaxStackSize() - 1) / material.getItemStack().getMaxStackSize();
            }
        }
        inventory = createInventory(Math.max(1, slots));
        updateBlockState();
        setChanged();
    }

    public int getColonyId()
    {
        return colonyId;
    }

    public int getWorkOrderId()
    {
        return workOrderId;
    }

    public BlockPos getBuilderPos()
    {
        return buildingPos;
    }

    /**
     * Whether the players were told that the pile is complete.
     */
    public boolean isAnnounced()
    {
        return announced;
    }

    public void setAnnounced()
    {
        announced = true;
        setChanged();
    }

    private static boolean matches(final ItemStorage material, final ItemStack stack)
    {
        return ItemStackUtils.compareItemStacksIgnoreStackSize(material.getItemStack(), stack, true, true);
    }

    private int stored(final ItemStorage material)
    {
        int count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++)
        {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (matches(material, stack))
            {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Each material still missing, with the amount missing.
     */
    public List<ItemStorage> missing()
    {
        final List<ItemStorage> missing = new ArrayList<>();
        for (final ItemStorage material : required)
        {
            final int amount = material.getAmount() - stored(material);
            if (amount > 0)
            {
                missing.add(new ItemStorage(material.getItemStack(), amount));
            }
        }
        return missing;
    }

    /**
     * Whether the builder may work from the pile: it is complete, or was once (the builder then takes from it).
     */
    public boolean isReady()
    {
        if (!started && isComplete())
        {
            started = true;
            setChanged();
        }
        return started;
    }

    public boolean isComplete()
    {
        return missing().isEmpty();
    }

    /**
     * Puts in as much of the stack as the work order still needs, taking it from the stack.
     *
     * @return the number of items put in.
     */
    public int deposit(final ItemStack stack)
    {
        for (final ItemStorage material : required)
        {
            if (!matches(material, stack))
            {
                continue;
            }
            final int wanted = Math.min(stack.getCount(), material.getAmount() - stored(material));
            if (wanted <= 0)
            {
                return 0;
            }
            ItemStack rest = stack.copyWithCount(wanted);
            depositing = true;
            try
            {
                for (int slot = 0; slot < inventory.getSlots() && !rest.isEmpty(); slot++)
                {
                    rest = inventory.insertItem(slot, rest, false);
                }
            }
            finally
            {
                depositing = false;
            }
            final int moved = wanted - rest.getCount();
            stack.shrink(moved);
            return moved;
        }
        return 0;
    }

    /**
     * Everything in the pile, taken out of it.
     */
    public List<ItemStack> takeAll()
    {
        final List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++)
        {
            final ItemStack stack = inventory.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty())
            {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    @Override
    protected void saveAdditional(@NotNull final CompoundTag tag, @NotNull final HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        tag.putInt(TAG_COLONY, colonyId);
        tag.putInt(TAG_WORK_ORDER, workOrderId);
        tag.putLong("building", buildingPos.asLong());
        tag.putBoolean(TAG_ANNOUNCED, announced);
        tag.putBoolean(TAG_STARTED, started);
        final ListTag list = new ListTag();
        for (final ItemStorage material : required)
        {
            final CompoundTag entry = new CompoundTag();
            entry.put(TAG_ITEM, material.getItemStack().save(provider));
            entry.putInt(TAG_COUNT, material.getAmount());
            list.add(entry);
        }
        tag.put(TAG_REQUIRED, list);
        tag.putInt(TAG_SIZE, inventory.getSlots());
        tag.put(TAG_INVENTORY, inventory.serializeNBT(provider));
    }

    @Override
    protected void loadAdditional(@NotNull final CompoundTag tag, @NotNull final HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        colonyId = tag.getInt(TAG_COLONY);
        workOrderId = tag.getInt(TAG_WORK_ORDER);
        buildingPos = BlockPos.of(tag.getLong("building"));
        announced = tag.getBoolean(TAG_ANNOUNCED);
        started = tag.getBoolean(TAG_STARTED);
        required.clear();
        for (final Tag element : tag.getList(TAG_REQUIRED, Tag.TAG_COMPOUND))
        {
            final CompoundTag entry = (CompoundTag) element;
            final ItemStack stack = ItemStack.parseOptional(provider, entry.getCompound(TAG_ITEM));
            if (!stack.isEmpty())
            {
                required.add(new ItemStorage(stack, entry.getInt(TAG_COUNT)));
            }
        }
        inventory = createInventory(Math.max(1, tag.getInt(TAG_SIZE)));
        inventory.deserializeNBT(provider, tag.getCompound(TAG_INVENTORY));
    }

    @Override
    public IItemHandler getItemHandlerCap(final Direction direction)
    {
        return inventory;
    }

    @Override
    public void setInWarehouse(final Boolean isInWarehouse)
    {
        // A pile belongs to its builder only.
    }

    @Override
    public int getFreeSlots()
    {
        return 0;
    }

    @Override
    public boolean hasItemStack(final ItemStack stack, final int count, final boolean ignoreDamage)
    {
        return getCount(stack, ignoreDamage, false) >= count;
    }

    @Override
    public boolean hasItemStorage(final ItemStorage storage, final int count)
    {
        return getCount(storage) >= count;
    }

    @Override
    public int getCount(final ItemStack stack, final boolean ignoreDamage, final boolean ignoreNBT)
    {
        return getItemCount(other -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, other, !ignoreDamage, !ignoreNBT));
    }

    @Override
    public int getCount(final ItemStorage storage)
    {
        return getItemCount(storage::equals);
    }

    @Override
    public boolean hasItemStack(@NotNull final Predicate<ItemStack> predicate)
    {
        return getItemCount(predicate) > 0;
    }

    @Override
    public boolean hasSimilarStack(@NotNull final ItemStack stack)
    {
        return getItemCount(other -> ItemStack.isSameItem(stack, other)) > 0;
    }

    @Override
    public void upgradeRackSize()
    {
        // Sized by its work order.
    }

    @Override
    public int getUpgradeSize()
    {
        return 0;
    }

    @Override
    public int getItemCount(final Predicate<ItemStack> predicate)
    {
        int count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++)
        {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && predicate.test(stack))
            {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public void updateItemStorage()
    {
        updateBlockState();
        setChanged();
    }

    @Override
    protected void updateBlockState()
    {
        if (level == null || level.isClientSide || !(getBlockState().getBlock() instanceof BlockMaterialPile))
        {
            return;
        }
        int needed = 0;
        int stored = 0;
        for (final ItemStorage material : required)
        {
            needed += material.getAmount();
            stored += Math.min(material.getAmount(), stored(material));
        }
        final int fill = needed == 0 ? 0 : Math.min(BlockMaterialPile.MAX_FILL, stored * (BlockMaterialPile.MAX_FILL + 1) / needed);
        if (getBlockState().getValue(BlockMaterialPile.FILL) != fill)
        {
            level.setBlock(worldPosition, getBlockState().setValue(BlockMaterialPile.FILL, fill), 3);
        }
    }

    @Override
    public AbstractTileEntityRack getOtherChest()
    {
        return null;
    }

    @Override
    public boolean isEmpty()
    {
        return getItemCount(stack -> true) == 0;
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.legacycolonies.material_pile");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int id, @NotNull final Inventory inv, @NotNull final Player player)
    {
        return null;
    }

    /**
     * Refuses every insertion but the player's (see {@link #deposit}).
     */
    private class PileInventory extends RackInventory
    {
        PileInventory(final int size)
        {
            super(size);
        }

        @NotNull
        @Override
        public ItemStack insertItem(final int slot, @NotNull final ItemStack stack, final boolean simulate)
        {
            return depositing ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public void setStackInSlot(final int slot, @NotNull final ItemStack stack)
        {
            final ItemStack current = getStackInSlot(slot);
            if (depositing || stack.isEmpty() || (ItemStack.isSameItemSameComponents(stack, current) && stack.getCount() <= current.getCount()))
            {
                super.setStackInSlot(slot, stack);
            }
        }

        @Override
        public boolean isItemValid(final int slot, @NotNull final ItemStack stack)
        {
            return depositing && super.isItemValid(slot, stack);
        }
    }
}
