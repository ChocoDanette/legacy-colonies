package com.chocodanette.legacycolonies.building;

import com.chocodanette.legacycolonies.block.BlockBuilderPost;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import net.minecraft.core.BlockPos;

/**
 * Builder's post building: a builder's hut without a structure.
 */
public class BuildingBuilderPost extends BuildingBuilder
{
    public BuildingBuilderPost(final IColony colony, final BlockPos pos)
    {
        super(colony, pos);
    }

    @Override
    public String getSchematicName()
    {
        return BlockBuilderPost.NAME;
    }
}
