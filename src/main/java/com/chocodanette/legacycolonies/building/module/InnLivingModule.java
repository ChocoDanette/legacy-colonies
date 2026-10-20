package com.chocodanette.legacycolonies.building.module;

import com.minecolonies.core.colony.buildings.modules.LivingBuildingModule;

/**
 * Living module of the inn: citizens without a house are moved in automatically, like in a house.
 */
public class InnLivingModule extends LivingBuildingModule
{
    /**
     * Beds at level 1; each further level adds {@link #BEDS_PER_LEVEL}.
     */
    public static final int BASE_BEDS      = 4;
    public static final int BEDS_PER_LEVEL = 2;

    @Override
    public int getModuleMax()
    {
        final int level = building.getBuildingLevel();
        return level > 0 ? BASE_BEDS + (level - 1) * BEDS_PER_LEVEL : 0;
    }
}
