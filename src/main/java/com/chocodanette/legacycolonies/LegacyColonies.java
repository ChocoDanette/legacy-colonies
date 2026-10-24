package com.chocodanette.legacycolonies;

import com.chocodanette.legacycolonies.colony.BuildModeHandler;
import com.chocodanette.legacycolonies.colony.BuilderPostHandler;
import com.chocodanette.legacycolonies.colony.CampfireInteractionHandler;
import com.chocodanette.legacycolonies.colony.HutPlacementHandler;
import com.chocodanette.legacycolonies.compat.CharcoalForgePlacementHandler;
import com.chocodanette.legacycolonies.compat.DaubedWattlePlacementHandler;
import com.chocodanette.legacycolonies.compat.FirepitPlacementHandler;
import com.chocodanette.legacycolonies.compat.RockAnvilPlacementHandler;
import com.chocodanette.legacycolonies.compat.ThatchBedPlacementHandler;
import com.chocodanette.legacycolonies.material.MaterialChoiceHandler;
import com.chocodanette.legacycolonies.network.BuildModesPayload;
import com.chocodanette.legacycolonies.network.BuildingRulesPayload;
import com.chocodanette.legacycolonies.network.ConfirmBuildingPayload;
import com.chocodanette.legacycolonies.network.MaterialChoicesPayload;
import com.chocodanette.legacycolonies.validation.BuildingRulesManager;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
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
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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
        modBus.addListener(this::onRegisterPayloads);
        NeoForge.EVENT_BUS.register(HutPlacementHandler.class);
        NeoForge.EVENT_BUS.register(MaterialChoiceHandler.class);
        NeoForge.EVENT_BUS.register(BuildModeHandler.class);
        NeoForge.EVENT_BUS.register(CampfireInteractionHandler.class);
        NeoForge.EVENT_BUS.register(BuildingRulesManager.class);
        NeoForge.EVENT_BUS.addListener(BuildingRulesPayload::onDatapackSync);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(LegacyColonies::addHutsToBuildingBlockEntity);
        event.enqueueWork(() -> {
            PlacementHandlers.add(new DaubedWattlePlacementHandler());
            PlacementHandlers.add(new FirepitPlacementHandler());
            PlacementHandlers.add(new CharcoalForgePlacementHandler());
            PlacementHandlers.add(new RockAnvilPlacementHandler());
            // Before Structurize's own bed handler, which would ask for a thatch bed item.
            PlacementHandlers.add(new ThatchBedPlacementHandler(), PlacementHandlers.BedPlacementHandler.class, PlacementHandlers.AddType.BEFORE);
        });
        IMinecoloniesAPI.getInstance().getEventBus().subscribe(ColonyCreatedModEvent.class, BuilderPostHandler::onColonyCreated);
        IMinecoloniesAPI.getInstance().getEventBus().subscribe(BuildingRemovedModEvent.class, MaterialChoiceHandler::onBuildingRemoved);
        IMinecoloniesAPI.getInstance().getEventBus().subscribe(BuildingRemovedModEvent.class, BuildModeHandler::onBuildingRemoved);
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
        blocks.add(ModBlocks.FORGE.get());
        blocks.add(ModBlocks.STABLE.get());
        blocks.add(ModBlocks.WAREHOUSE.get());
        type.validBlocks = blocks;
    }

    private void onRegisterPayloads(final RegisterPayloadHandlersEvent event)
    {
        event.registrar("1")
          .playToClient(MaterialChoicesPayload.TYPE, MaterialChoicesPayload.STREAM_CODEC, MaterialChoicesPayload::handle)
          .playToClient(BuildModesPayload.TYPE, BuildModesPayload.STREAM_CODEC, BuildModesPayload::handle)
          .playToClient(BuildingRulesPayload.TYPE, BuildingRulesPayload.STREAM_CODEC, BuildingRulesPayload::handle)
          .playToServer(ConfirmBuildingPayload.TYPE, ConfirmBuildingPayload.STREAM_CODEC, ConfirmBuildingPayload::handle);
    }

    private void onCreativeTab(final BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
        {
            event.accept(ModBlocks.COLONY_CAMPFIRE_ITEM.get());
            event.accept(ModBlocks.INN_ITEM.get());
            event.accept(ModBlocks.FORGE_ITEM.get());
            event.accept(ModBlocks.STABLE_ITEM.get());
            event.accept(ModBlocks.WAREHOUSE_ITEM.get());
        }
    }
}
