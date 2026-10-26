package com.chocodanette.legacycolonies.building.module;

/**
 * Living module of the inn: citizens without a house are moved in automatically, like in a house. Level 1 is a bare
 * shelter; each further level holds one more bed (2 beds at level 3, 3 at level 4).
 */
public class InnLivingModule extends LegacyLivingModule
{
    @Override
    protected int maxBeds(final int level)
    {
        return level - 1;
    }
}
