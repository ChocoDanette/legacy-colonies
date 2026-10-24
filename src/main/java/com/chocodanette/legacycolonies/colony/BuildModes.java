package com.chocodanette.legacycolonies.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Build mode of each hut of a level, keyed by the position of the hut block. A hut without an entry has not been built yet.
 */
public class BuildModes extends SavedData
{
    private static final String NAME          = "legacycolonies_build_modes";
    private static final String TAG_BUILDINGS = "buildings";
    private static final String TAG_POS       = "pos";
    private static final String TAG_MODE      = "mode";

    private final Map<BlockPos, BuildMode> modes = new HashMap<>();

    public static BuildModes get(final ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(new Factory<>(BuildModes::new, BuildModes::load, null), NAME);
    }

    @Nullable
    public BuildMode getMode(final BlockPos building)
    {
        return modes.get(building);
    }

    public Map<BlockPos, BuildMode> getAll()
    {
        return modes;
    }

    /**
     * @return false when the hut already had a mode: it is chosen only once.
     */
    public boolean choose(final BlockPos building, final BuildMode mode)
    {
        if (modes.putIfAbsent(building.immutable(), mode) != null)
        {
            return false;
        }
        setDirty();
        return true;
    }

    public boolean clear(final BlockPos building)
    {
        if (modes.remove(building) == null)
        {
            return false;
        }
        setDirty();
        return true;
    }

    private static BuildModes load(final CompoundTag tag, final HolderLookup.Provider provider)
    {
        final BuildModes data = new BuildModes();
        for (final Tag entry : tag.getList(TAG_BUILDINGS, Tag.TAG_COMPOUND))
        {
            final CompoundTag building = (CompoundTag) entry;
            final BlockPos pos = NbtUtils.readBlockPos(building, TAG_POS).orElse(null);
            if (pos == null)
            {
                continue;
            }
            for (final BuildMode mode : BuildMode.values())
            {
                if (mode.name().equals(building.getString(TAG_MODE)))
                {
                    data.modes.put(pos, mode);
                }
            }
        }
        return data;
    }

    @NotNull
    @Override
    public CompoundTag save(@NotNull final CompoundTag tag, @NotNull final HolderLookup.Provider provider)
    {
        final ListTag buildings = new ListTag();
        modes.forEach((pos, mode) -> {
            final CompoundTag building = new CompoundTag();
            building.put(TAG_POS, NbtUtils.writeBlockPos(pos));
            building.putString(TAG_MODE, mode.name());
            buildings.add(building);
        });
        tag.put(TAG_BUILDINGS, buildings);
        return tag;
    }
}
