package com.chocodanette.legacycolonies;

import com.chocodanette.legacycolonies.colony.BuilderPostHandler;
import com.chocodanette.legacycolonies.colony.CampfirePlacementHandler;
import com.chocodanette.legacycolonies.material.MaterialChoiceHandler;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.eventbus.events.colony.ColonyCreatedModEvent;
import com.minecolonies.api.eventbus.events.colony.buildings.BuildingRemovedModEvent;
import com.minecolonies.api.tileentities.MinecoloniesTileEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * MineColonies addon for the TerraFirmaCraft Legacy modpack.
 */
@Mod(LegacyColonies.MOD_ID)
public class LegacyColonies
{
    public static final String MOD_ID = "legacycolonies";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LegacyColonies(final IEventBus modBus)
    {
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);
        ModBuildings.BUILDINGS.register(modBus);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onCreativeTab);
        NeoForge.EVENT_BUS.register(CampfirePlacementHandler.class);
        NeoForge.EVENT_BUS.register(MaterialChoiceHandler.class);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(LegacyColonies::addHutsToBuildingBlockEntity);
        IMinecoloniesAPI.getInstance().getEventBus().subscribe(ColonyCreatedModEvent.class, BuilderPostHandler::onColonyCreated);
        IMinecoloniesAPI.getInstance().getEventBus().subscribe(BuildingRemovedModEvent.class, MaterialChoiceHandler::onBuildingRemoved);
        LOGGER.info("Legacy Colonies loaded");
    }

    /**
     * MineColonies builds its hut block entity type from its own huts only: add ours to it.
     */
    private static void addHutsToBuildingBlockEntity()
    {
        final BlockEntityType<?> type = MinecoloniesTileEntities.BUILDING.get();
        final Set<Block> blocks = new HashSet<>(type.validBlocks);
        blocks.add(ModBlocks.COLONY_CAMPFIRE.get());
        blocks.add(ModBlocks.BUILDER_POST.get());
        blocks.add(ModBlocks.INN.get());
        type.validBlocks = blocks;
    }

    private void onCreativeTab(final BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
        {
            event.accept(ModBlocks.COLONY_CAMPFIRE_ITEM.get());
            event.accept(ModBlocks.INN_ITEM.get());
        }
    }
}
