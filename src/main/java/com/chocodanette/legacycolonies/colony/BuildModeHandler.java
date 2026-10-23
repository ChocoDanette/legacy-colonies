package com.chocodanette.legacycolonies.colony;

import com.chocodanette.legacycolonies.network.BuildModesPayload;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.workorders.IWorkOrder;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.api.eventbus.events.colony.buildings.BuildingRemovedModEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Records who builds each hut and keeps the clients informed, so the hut window shows only the matching button.
 */
public final class BuildModeHandler
{
    private BuildModeHandler()
    {
    }

    /**
     * Choose the build mode of a hut, unless it already has one.
     */
    public static void choose(final ServerLevel level, final BlockPos building, final BuildMode mode)
    {
        if (BuildModes.get(level).choose(building, mode))
        {
            BuildModesPayload.sendToLevel(level);
        }
    }

    /**
     * A building work order given to the builder (built or upgraded from the plan) makes the hut a builder's hut.
     */
    public static void onWorkOrderAdded(final IColony colony, final IWorkOrder workOrder)
    {
        if ((workOrder.getWorkOrderType() == WorkOrderType.BUILD || workOrder.getWorkOrderType() == WorkOrderType.UPGRADE)
              && colony != null
              && colony.getWorld() instanceof final ServerLevel level)
        {
            choose(level, workOrder.getLocation(), BuildMode.BUILDER);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof final ServerPlayer player)
        {
            BuildModesPayload.sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof final ServerPlayer player)
        {
            BuildModesPayload.sendTo(player);
        }
    }

    /**
     * A hut placed again starts without a mode.
     */
    public static void onBuildingRemoved(final BuildingRemovedModEvent event)
    {
        if (event.getBuilding().getColony().getWorld() instanceof final ServerLevel level
              && BuildModes.get(level).clear(event.getBuilding().getPosition()))
        {
            BuildModesPayload.sendToLevel(level);
        }
    }
}
