package com.chocodanette.legacycolonies.validation;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * The hut levels the server lets players confirm by hand, as last sent by the server.
 */
public final class ClientBuildingRules
{
    private static volatile Set<String> levels = Set.of();

    private ClientBuildingRules()
    {
    }

    public static void set(final Set<String> newLevels)
    {
        levels = newLevels;
    }

    public static boolean canConfirm(final ResourceLocation building, final int level)
    {
        return levels.contains(BuildingRulesManager.key(building, level));
    }
}
