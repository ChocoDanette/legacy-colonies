package com.chocodanette.legacycolonies;

import com.chocodanette.legacycolonies.block.BlockBuilderPost;
import com.chocodanette.legacycolonies.block.BlockColonyCampfire;
import com.chocodanette.legacycolonies.block.BlockForge;
import com.chocodanette.legacycolonies.block.BlockGranary;
import com.chocodanette.legacycolonies.block.BlockHouse;
import com.chocodanette.legacycolonies.block.BlockInn;
import com.chocodanette.legacycolonies.block.BlockStable;
import com.chocodanette.legacycolonies.block.BlockWarehouse;
import com.minecolonies.api.items.ItemBlockHut;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Blocks and items of the addon.
 */
public final class ModBlocks
{
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LegacyColonies.MOD_ID);
    public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems(LegacyColonies.MOD_ID);

    public static final DeferredBlock<BlockColonyCampfire> COLONY_CAMPFIRE = BLOCKS.register(BlockColonyCampfire.NAME, BlockColonyCampfire::new);
    public static final DeferredBlock<BlockBuilderPost>    BUILDER_POST    = BLOCKS.register(BlockBuilderPost.NAME, BlockBuilderPost::new);
    public static final DeferredBlock<BlockInn>            INN             = BLOCKS.register(BlockInn.NAME, BlockInn::new);
    public static final DeferredBlock<BlockHouse>          HOUSE           = BLOCKS.register(BlockHouse.NAME, BlockHouse::new);
    public static final DeferredBlock<BlockGranary>        GRANARY         = BLOCKS.register(BlockGranary.NAME, BlockGranary::new);
    public static final DeferredBlock<BlockForge>          FORGE           = BLOCKS.register(BlockForge.NAME, BlockForge::new);
    public static final DeferredBlock<BlockStable>         STABLE          = BLOCKS.register(BlockStable.NAME, BlockStable::new);
    public static final DeferredBlock<BlockWarehouse>      WAREHOUSE       = BLOCKS.register(BlockWarehouse.NAME, BlockWarehouse::new);

    public static final DeferredItem<Item> COLONY_CAMPFIRE_ITEM =
      ITEMS.register(BlockColonyCampfire.NAME, () -> new ItemBlockHut(COLONY_CAMPFIRE.get(), new Item.Properties()));
    public static final DeferredItem<Item> INN_ITEM =
      ITEMS.register(BlockInn.NAME, () -> new ItemBlockHut(INN.get(), new Item.Properties()));
    public static final DeferredItem<Item> HOUSE_ITEM =
      ITEMS.register(BlockHouse.NAME, () -> new ItemBlockHut(HOUSE.get(), new Item.Properties()));
    public static final DeferredItem<Item> GRANARY_ITEM =
      ITEMS.register(BlockGranary.NAME, () -> new ItemBlockHut(GRANARY.get(), new Item.Properties()));
    public static final DeferredItem<Item> FORGE_ITEM =
      ITEMS.register(BlockForge.NAME, () -> new ItemBlockHut(FORGE.get(), new Item.Properties()));
    public static final DeferredItem<Item> STABLE_ITEM =
      ITEMS.register(BlockStable.NAME, () -> new ItemBlockHut(STABLE.get(), new Item.Properties()));
    public static final DeferredItem<Item> WAREHOUSE_ITEM =
      ITEMS.register(BlockWarehouse.NAME, () -> new ItemBlockHut(WAREHOUSE.get(), new Item.Properties()));

    private ModBlocks()
    {
    }
}
