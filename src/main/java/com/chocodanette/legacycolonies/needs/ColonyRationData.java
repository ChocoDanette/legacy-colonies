package com.chocodanette.legacycolonies.needs;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Per colony of a level: the last day rations were handed out, and how many days in a row each citizen went without one.
 */
public class ColonyRationData extends SavedData
{
    private static final String NAME         = "legacycolonies_colony_rations";
    private static final String TAG_COLONIES = "colonies";
    private static final String TAG_ID       = "id";
    private static final String TAG_LAST_DAY = "last_day";
    private static final String TAG_HUNGRY   = "hungry";
    private static final String TAG_CITIZEN  = "citizen";
    private static final String TAG_DAYS     = "days";

    private final Map<Integer, Long>                  lastDays = new HashMap<>();
    private final Map<Integer, Map<Integer, Integer>> hungry   = new HashMap<>();

    public static ColonyRationData get(final ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ColonyRationData::new, ColonyRationData::load, null), NAME);
    }

    /**
     * @return the last day rations were handed out, or -1 when they never were.
     */
    public long getLastDay(final int colony)
    {
        return lastDays.getOrDefault(colony, -1L);
    }

    public void setLastDay(final int colony, final long day)
    {
        lastDays.put(colony, day);
        setDirty();
    }

    /**
     * @return days in a row the citizen went without a ration, 0 when it had one yesterday.
     */
    public int getHungryDays(final int colony, final int citizen)
    {
        final Map<Integer, Integer> citizens = hungry.get(colony);
        return citizens == null ? 0 : citizens.getOrDefault(citizen, 0);
    }

    public void setHungryDays(final int colony, final int citizen, final int days)
    {
        if (days <= 0)
        {
            final Map<Integer, Integer> citizens = hungry.get(colony);
            if (citizens != null && citizens.remove(citizen) != null)
            {
                setDirty();
            }
            return;
        }
        hungry.computeIfAbsent(colony, id -> new HashMap<>()).put(citizen, days);
        setDirty();
    }

    /**
     * Citizens of the colony that went without a ration yesterday.
     */
    public int hungryCitizens(final int colony)
    {
        final Map<Integer, Integer> citizens = hungry.get(colony);
        return citizens == null ? 0 : citizens.size();
    }

    public void clearHungry(final int colony)
    {
        if (hungry.remove(colony) != null)
        {
            setDirty();
        }
    }

    private static ColonyRationData load(final CompoundTag tag, final HolderLookup.Provider provider)
    {
        final ColonyRationData data = new ColonyRationData();
        for (final Tag entry : tag.getList(TAG_COLONIES, Tag.TAG_COMPOUND))
        {
            final CompoundTag colony = (CompoundTag) entry;
            final int id = colony.getInt(TAG_ID);
            data.lastDays.put(id, colony.getLong(TAG_LAST_DAY));
            for (final Tag hungryEntry : colony.getList(TAG_HUNGRY, Tag.TAG_COMPOUND))
            {
                final CompoundTag citizen = (CompoundTag) hungryEntry;
                data.hungry.computeIfAbsent(id, key -> new HashMap<>()).put(citizen.getInt(TAG_CITIZEN), citizen.getInt(TAG_DAYS));
            }
        }
        return data;
    }

    @NotNull
    @Override
    public CompoundTag save(@NotNull final CompoundTag tag, @NotNull final HolderLookup.Provider provider)
    {
        final ListTag colonies = new ListTag();
        for (final Map.Entry<Integer, Long> entry : lastDays.entrySet())
        {
            final CompoundTag colony = new CompoundTag();
            colony.putInt(TAG_ID, entry.getKey());
            colony.putLong(TAG_LAST_DAY, entry.getValue());
            final ListTag citizens = new ListTag();
            for (final Map.Entry<Integer, Integer> citizen : hungry.getOrDefault(entry.getKey(), Map.of()).entrySet())
            {
                final CompoundTag citizenTag = new CompoundTag();
                citizenTag.putInt(TAG_CITIZEN, citizen.getKey());
                citizenTag.putInt(TAG_DAYS, citizen.getValue());
                citizens.add(citizenTag);
            }
            colony.put(TAG_HUNGRY, citizens);
            colonies.add(colony);
        }
        tag.put(TAG_COLONIES, colonies);
        return tag;
    }
}
