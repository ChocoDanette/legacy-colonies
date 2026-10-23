package com.chocodanette.legacycolonies.block;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.ModBuildings;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Hut block of the stable, a shelter for animals. No worker and no animal of its own.
 */
public class BlockStable extends AbstractBlockHut<BlockStable>
{
    public static final String NAME = "stable";

    @NotNull
    @Override
    public String getHutName()
    {
        return NAME;
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, NAME);
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.STABLE.get();
    }
}
