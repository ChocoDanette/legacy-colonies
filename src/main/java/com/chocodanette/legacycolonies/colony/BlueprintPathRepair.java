package com.chocodanette.legacycolonies.colony;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.ldtteam.structurize.storage.ServerFutureProcessor;
import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

/**
 * MineColonies does not know the Legacy huts: a hut without a plan path gets "null1.blueprint", and every load of that
 * plan (prestige, corners) logs an error. A hut built by hand has no plan, so it skips these loads. A hut for the builder
 * gets the path of its plan in the pack back, at its level, the same lookup as {@link HutPlacementHandler}.
 */
public final class BlueprintPathRepair
{
    /**
     * Huts whose plan is being looked up, so that the lookup runs once.
     */
    private static final Set<BlockPos> PENDING = new HashSet<>();

    private BlueprintPathRepair()
    {
    }

    /**
     * @return true when the building is a Legacy hut without a real plan path, so its plan must not be loaded.
     */
    public static boolean hasNoPlan(final IBuilding building)
    {
        if (!LegacyColonies.MOD_ID.equals(building.getBuildingType().getRegistryName().getNamespace()))
        {
            return false;
        }
        final String path = building.getBlueprintPath();
        if (path != null && !path.isEmpty() && !path.substring(path.lastIndexOf('/') + 1).startsWith("null"))
        {
            return false;
        }
        if (building.getColony().getWorld() instanceof final ServerLevel level
              && BuildModes.get(level).getMode(building.getPosition()) != BuildMode.HAND)
        {
            repair(building, level);
        }
        return true;
    }

    private static void repair(final IBuilding building, final ServerLevel level)
    {
        final StructurePackMeta pack = StructurePacks.getStructurePack(building.getStructurePack());
        if (pack == null || !PENDING.add(building.getPosition()))
        {
            return;
        }
        final Block hutBlock = building.getBuildingType().getBuildingBlock();
        ServerFutureProcessor.queueBlueprint(new ServerFutureProcessor.BlueprintProcessingData(
          StructurePacks.findBlueprintFuture(pack.getName(),
            blueprint -> blueprint.getBlockState(blueprint.getPrimaryBlockOffset()).getBlock() == hutBlock,
            level.registryAccess()),
          level,
          blueprint -> {
              PENDING.remove(building.getPosition());
              if (blueprint == null || building.getTileEntity() == null)
              {
                  return;
              }
              final String folder = pack.getPath().relativize(blueprint.getFilePath()).toString().replace('\\', '/');
              final String name = blueprint.getFileName();
              final String path = (folder.isEmpty() ? "" : folder + "/") + name.substring(0, name.length() - 1) + Math.max(1, building.getBuildingLevel()) + ".blueprint";
              if (Files.exists(pack.getPath().resolve(path)))
              {
                  building.setBlueprintPath(path);
              }
          }));
    }
}
