package com.chocodanette.legacycolonies;

import com.chocodanette.legacycolonies.block.BlockBuilderPost;
import com.chocodanette.legacycolonies.block.BlockColonyCampfire;
import com.chocodanette.legacycolonies.block.BlockInn;
import com.chocodanette.legacycolonies.building.BuildingBuilderPost;
import com.chocodanette.legacycolonies.building.BuildingColonyCampfire;
import com.chocodanette.legacycolonies.building.module.InnLivingModule;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.core.colony.buildings.DefaultBuildingInstance;
import com.minecolonies.core.colony.buildings.moduleviews.LivingBuildingModuleView;
import com.minecolonies.core.colony.buildings.views.EmptyView;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.minecolonies.core.colony.buildings.modules.BuildingModules.*;

/**
 * MineColonies building types of the addon.
 */
public final class ModBuildings
{
    public static final DeferredRegister<BuildingEntry> BUILDINGS = DeferredRegister.create(CommonMinecoloniesAPIImpl.BUILDINGS, LegacyColonies.MOD_ID);

    /**
     * The colony campfire: a town hall, so MineColonies treats it as the colony centre.
     */
    public static final DeferredHolder<BuildingEntry, BuildingEntry> COLONY_CAMPFIRE = BUILDINGS.register(BlockColonyCampfire.NAME, () -> new BuildingEntry.Builder()
      .setBuildingBlock(ModBlocks.COLONY_CAMPFIRE.get())
      .setBuildingProducer(BuildingColonyCampfire::new)
      .setBuildingViewProducer(() -> BuildingTownHall.View::new)
      .setRegistryName(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, BlockColonyCampfire.NAME))
      .addBuildingModuleProducer(TOWNHALL_SETTINGS)
      .createBuildingEntry());

    /**
     * The invisible builder's post above the campfire: a builder's hut, so the first citizen can work as builder.
     */
    public static final DeferredHolder<BuildingEntry, BuildingEntry> BUILDER_POST = BUILDINGS.register(BlockBuilderPost.NAME, () -> new BuildingEntry.Builder()
      .setBuildingBlock(ModBlocks.BUILDER_POST.get())
      .setBuildingProducer(BuildingBuilderPost::new)
      .setBuildingViewProducer(() -> BuildingBuilder.View::new)
      .setRegistryName(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, BlockBuilderPost.NAME))
      .addBuildingModuleProducer(BUILDER_WORK)
      .addBuildingModuleProducer(BUILDER_CRAFT)
      .addBuildingModuleProducer(BUILDING_RESOURCES)
      .addBuildingModuleProducer(BUILDER_SETTINGS)
      .addBuildingModuleProducer(WORKORDER_VIEW)
      .addBuildingModuleProducer(MIN_STOCK)
      .addBuildingModuleProducer(STATS_MODULE)
      .createBuildingEntry());

    /**
     * Living module of the inn, registered under its own key so it is not mistaken for a house's.
     */
    public static final BuildingEntry.ModuleProducer<InnLivingModule, LivingBuildingModuleView> INN_LIVING =
      new BuildingEntry.ModuleProducer<>("inn_living", InnLivingModule::new, () -> LivingBuildingModuleView::new);

    /**
     * The inn: shelter for the player and for citizens without a house. Unlike MineColonies' tavern, it takes no visitors.
     */
    public static final DeferredHolder<BuildingEntry, BuildingEntry> INN = BUILDINGS.register(BlockInn.NAME, () -> new BuildingEntry.Builder()
      .setBuildingBlock(ModBlocks.INN.get())
      .setBuildingProducer((colony, pos) -> new DefaultBuildingInstance(colony, pos, BlockInn.NAME, 5))
      .setBuildingViewProducer(() -> EmptyView::new)
      .setRegistryName(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, BlockInn.NAME))
      .addBuildingModuleProducer(INN_LIVING)
      .addBuildingModuleProducer(BED)
      .addBuildingModuleProducer(STATS_MODULE)
      .createBuildingEntry());

    private ModBuildings()
    {
    }
}
