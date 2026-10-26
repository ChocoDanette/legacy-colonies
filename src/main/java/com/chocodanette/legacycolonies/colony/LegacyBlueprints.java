package com.chocodanette.legacycolonies.colony;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Plans of the Legacy huts in the modpack's "Legacy" structure pack. A plan is the file {@code <hut><level>.blueprint}
 * in any folder of the pack (all the levels of a hut in the same folder), so it is found right away, by its name,
 * without loading blueprints in the background.
 * <p>
 * A Legacy hut always takes its plans from this pack, whatever the colony's style was when it was placed: MineColonies
 * gives a hut the colony's style, and the other packs have no plan for the Legacy huts.
 */
public final class LegacyBlueprints
{
    public static final String PACK = "Legacy";

    private LegacyBlueprints()
    {
    }

    /**
     * @return the Legacy pack, or null when it is not installed.
     */
    @Nullable
    public static StructurePackMeta pack()
    {
        return StructurePacks.hasPack(PACK) ? StructurePacks.getStructurePack(PACK) : null;
    }

    public static boolean isLegacyHut(final BuildingEntry type)
    {
        return LegacyColonies.MOD_ID.equals(type.getRegistryName().getNamespace());
    }

    /**
     * @return the path of the hut's plan at this level in the pack, as MineColonies stores it (relative to the pack,
     * with "/"), or null when the pack has no such plan.
     */
    @Nullable
    public static String find(final StructurePackMeta pack, final BuildingEntry type, final int level)
    {
        final String file = type.getRegistryName().getPath() + level + ".blueprint";
        try (Stream<Path> files = Files.walk(pack.getPath()))
        {
            return files.filter(path -> path.getFileName() != null && path.getFileName().toString().equals(file))
                     .findFirst()
                     .map(path -> pack.getPath().relativize(path).toString().replace('\\', '/'))
                     .orElse(null);
        }
        catch (final IOException | UncheckedIOException e)
        {
            LegacyColonies.LOGGER.warn("Could not look for {} in the {} pack", file, PACK, e);
            return null;
        }
    }

    /**
     * Give a Legacy hut the Legacy pack and the path of its plan at its current level (level 1 for a hut not built yet),
     * on the building and on the hut block, so that MineColonies finds its next level next to it.
     *
     * @return true when the hut has its plan now, or is not a Legacy hut, or the pack is not installed.
     */
    public static boolean ensurePlan(final IBuilding building)
    {
        final StructurePackMeta pack = pack();
        if (!isLegacyHut(building.getBuildingType()) || pack == null)
        {
            return true;
        }
        final String path = find(pack, building.getBuildingType(), Math.max(1, building.getBuildingLevel()));
        if (path == null)
        {
            return false;
        }
        if (!PACK.equals(building.getStructurePack()))
        {
            building.setStructurePack(PACK);
        }
        if (!path.equals(building.getBlueprintPath()))
        {
            building.setBlueprintPath(path);
        }
        if (building.getTileEntity() instanceof final TileEntityColonyBuilding hut)
        {
            hut.setStructurePack(pack);
            hut.setBlueprintPath(path);
        }
        return true;
    }

    /**
     * Get a Legacy hut ready for the builder to bring it to this level.
     *
     * @return why the builder cannot, or null when it can (or the hut is not a Legacy hut).
     */
    @Nullable
    public static Component prepareForBuilder(final IBuilding building, final int target)
    {
        final StructurePackMeta pack = pack();
        if (!isLegacyHut(building.getBuildingType()) || pack == null)
        {
            return null;
        }
        if (!ensurePlan(building) || find(pack, building.getBuildingType(), target) == null)
        {
            return Component.translatable("com.legacycolonies.plan.missing",
              Component.translatable(building.getBuildingType().getTranslationKey()), target, PACK);
        }
        return null;
    }
}
