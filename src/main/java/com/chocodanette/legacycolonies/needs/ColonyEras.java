package com.chocodanette.legacycolonies.needs;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Era of each colony of a level and the game time its next citizen arrives, keyed by colony id.
 */
public class ColonyEras extends SavedData
{
    private static final String NAME             = "legacycolonies_colony_eras";
    private static final String TAG_COLONIES     = "colonies";
    private static final String TAG_ID           = "id";
    private static final String TAG_ERA          = "era";
    private static final String TAG_NEXT_ARRIVAL = "next_arrival";

    private final Map<Integer, Integer> eras         = new HashMap<>();
    private final Map<Integer, Long>    nextArrivals = new HashMap<>();

    public static ColonyEras get(final ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ColonyEras::new, ColonyEras::load, null), NAME);
    }

    /**
     * @return 1 (primitive) until the modpack gives the colony an era.
     */
    public int getEra(final int colony)
    {
        return eras.getOrDefault(colony, 1);
    }

    /**
     * Eras only move forward.
     *
     * @return true when the era changed.
     */
    public boolean raiseEra(final int colony, final int era)
    {
        if (era <= getEra(colony))
        {
            return false;
        }
        eras.put(colony, era);
        setDirty();
        return true;
    }

    /**
     * @return the game time the next citizen arrives, or 0 when no arrival is waiting.
     */
    public long getNextArrival(final int colony)
    {
        return nextArrivals.getOrDefault(colony, 0L);
    }

    public void setNextArrival(final int colony, final long gameTime)
    {
        final Long previous = gameTime == 0 ? nextArrivals.remove(colony) : nextArrivals.put(colony, gameTime);
        if (previous == null ? gameTime != 0 : previous != gameTime)
        {
            setDirty();
        }
    }

    private static ColonyEras load(final CompoundTag tag, final HolderLookup.Provider provider)
    {
        final ColonyEras data = new ColonyEras();
        for (final Tag entry : tag.getList(TAG_COLONIES, Tag.TAG_COMPOUND))
        {
            final CompoundTag colony = (CompoundTag) entry;
            final int id = colony.getInt(TAG_ID);
            if (colony.contains(TAG_ERA))
            {
                data.eras.put(id, colony.getInt(TAG_ERA));
            }
            if (colony.getLong(TAG_NEXT_ARRIVAL) != 0)
            {
                data.nextArrivals.put(id, colony.getLong(TAG_NEXT_ARRIVAL));
            }
        }
        return data;
    }

    @NotNull
    @Override
    public CompoundTag save(@NotNull final CompoundTag tag, @NotNull final HolderLookup.Provider provider)
    {
        final Set<Integer> ids = new HashSet<>(eras.keySet());
        ids.addAll(nextArrivals.keySet());
        final ListTag colonies = new ListTag();
        for (final int id : ids)
        {
            final CompoundTag colony = new CompoundTag();
            colony.putInt(TAG_ID, id);
            if (eras.containsKey(id))
            {
                colony.putInt(TAG_ERA, eras.get(id));
            }
            colony.putLong(TAG_NEXT_ARRIVAL, getNextArrival(id));
            colonies.add(colony);
        }
        tag.put(TAG_COLONIES, colonies);
        return tag;
    }
}
