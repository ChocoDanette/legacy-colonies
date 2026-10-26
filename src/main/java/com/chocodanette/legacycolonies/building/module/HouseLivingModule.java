package com.chocodanette.legacycolonies.building.module;

/**
 * Living module of the house: one bed per level (1 bed at level 1, 2 at level 2).
 */
public class HouseLivingModule extends LegacyLivingModule
{
    @Override
    protected int maxBeds(final int level)
    {
        return level;
    }
}
