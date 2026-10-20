package com.chocodanette.legacycolonies.material;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Materials chosen for each building of a level, keyed by the position of the hut block.
 */
public class MaterialChoices extends SavedData
{
    private static final String NAME          = "legacycolonies_materials";
    private static final String TAG_BUILDINGS = "buildings";
    private static final String TAG_POS       = "pos";

    private final Map<BlockPos, EnumMap<MaterialSlot, ResourceLocation>> choices = new HashMap<>();

    public static MaterialChoices get(final ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(new Factory<>(MaterialChoices::new, MaterialChoices::load, null), NAME);
    }

    @Nullable
    public Map<MaterialSlot, ResourceLocation> getChoices(final BlockPos building)
    {
        return choices.get(building);
    }

    public Map<BlockPos, EnumMap<MaterialSlot, ResourceLocation>> getAll()
    {
        return choices;
    }

    public void choose(final BlockPos building, final MaterialSlot slot, final ResourceLocation material)
    {
        choices.computeIfAbsent(building.immutable(), p -> new EnumMap<>(MaterialSlot.class)).put(slot, material);
        setDirty();
    }

    public void clear(final BlockPos building)
    {
        if (choices.remove(building) != null)
        {
            setDirty();
        }
    }

    private static MaterialChoices load(final CompoundTag tag, final HolderLookup.Provider provider)
    {
        final MaterialChoices data = new MaterialChoices();
        for (final Tag entry : tag.getList(TAG_BUILDINGS, Tag.TAG_COMPOUND))
        {
            final CompoundTag building = (CompoundTag) entry;
            final BlockPos pos = NbtUtils.readBlockPos(building, TAG_POS).orElse(null);
            if (pos == null)
            {
                continue;
            }
            for (final MaterialSlot slot : MaterialSlot.values())
            {
                final ResourceLocation material = ResourceLocation.tryParse(building.getString(slot.getId()));
                if (building.contains(slot.getId()) && material != null)
                {
                    data.choices.computeIfAbsent(pos, p -> new EnumMap<>(MaterialSlot.class)).put(slot, material);
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
        choices.forEach((pos, materials) -> {
            final CompoundTag building = new CompoundTag();
            building.put(TAG_POS, NbtUtils.writeBlockPos(pos));
            materials.forEach((slot, material) -> building.putString(slot.getId(), material.toString()));
            buildings.add(building);
        });
        tag.put(TAG_BUILDINGS, buildings);
        return tag;
    }
}
