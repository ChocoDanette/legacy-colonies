package com.chocodanette.legacycolonies.colony;

import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.server.level.ServerLevel;

/**
 * MineColonies does not know the Legacy huts: a hut without a plan path gets "null1.blueprint", and every load of that
 * plan (prestige, corners) logs an error. A hut built by hand has no plan, so it skips these loads. A hut for the builder
 * gets the Legacy pack and the path of its plan back, at its level (see {@link LegacyBlueprints#ensurePlan}).
 */
public final class BlueprintPathRepair
{
    private BlueprintPathRepair()
    {
    }

    /**
     * @return true when the building is a Legacy hut without a real plan path, so its plan must not be loaded.
     */
    public static boolean hasNoPlan(final IBuilding building)
    {
        if (!LegacyBlueprints.isLegacyHut(building.getBuildingType()))
        {
            return false;
        }
        final String path = building.getBlueprintPath();
        final boolean noPath = path == null || path.isEmpty() || path.substring(path.lastIndexOf('/') + 1).startsWith("null");
        final boolean otherPack = LegacyBlueprints.pack() != null && !LegacyBlueprints.PACK.equals(building.getStructurePack());
        if (!noPath && !otherPack)
        {
            return false;
        }
        if (building.getColony().getWorld() instanceof final ServerLevel level
              && BuildModes.get(level).getMode(building.getPosition()) != BuildMode.HAND
              && LegacyBlueprints.ensurePlan(building))
        {
            return LegacyBlueprints.pack() == null && noPath;
        }
        return noPath;
    }
}
