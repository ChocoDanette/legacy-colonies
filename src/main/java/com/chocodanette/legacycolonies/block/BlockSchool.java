package com.chocodanette.legacycolonies.block;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.ModBuildings;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Hut block of the school, where the player chooses the traits passed on to the heir.
 */
public class BlockSchool extends AbstractBlockHut<BlockSchool>
{
    public static final String NAME = "school";

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
        return ModBuildings.SCHOOL.get();
    }
}
