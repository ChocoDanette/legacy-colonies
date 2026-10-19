package com.chocodanette.legacycolonies.building;

import com.chocodanette.legacycolonies.block.BlockColonyCampfire;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import net.minecraft.core.BlockPos;

/**
 * Colony campfire building: a town hall with its own blueprint name ({@code colony_campfire1..5}).
 */
public class BuildingColonyCampfire extends BuildingTownHall
{
    public BuildingColonyCampfire(final IColony colony, final BlockPos pos)
    {
        super(colony, pos);
    }

    @Override
    public String getSchematicName()
    {
        return BlockColonyCampfire.NAME;
    }
}
