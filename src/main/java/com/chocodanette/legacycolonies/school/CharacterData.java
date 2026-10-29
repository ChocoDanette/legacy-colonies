package com.chocodanette.legacycolonies.school;

import com.chocodanette.legacycolonies.LegacyColonies;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The player's character, kept by the modpack's KubeJS script in the player's KubeJS data ({@code player.persistentData}):
 * first name, traits, spouse, and the school's changes for the heir. Read through reflection: KubeJS is not a
 * dependency of the addon.
 */
public final class CharacterData
{
    public static final String FIRST_NAME = "legacy_first_name";
    public static final String TRAITS = "legacy_traits";
    public static final String SPOUSE = "legacy_spouse";
    /**
     * The school's changes, e.g. {@code add:fort,drop:delicat}: read by the KubeJS script when the heir is made.
     */
    public static final String SCHOOL = "legacy_school";

    private static final String ADD = "add:";
    private static final String DROP = "drop:";

    private static Method persistentData;
    private static boolean missingLogged;

    private CharacterData()
    {
    }

    /**
     * The player's KubeJS data, or an empty tag without KubeJS (changes to it are then lost).
     */
    public static CompoundTag of(final Player player)
    {
        try
        {
            if (persistentData == null)
            {
                persistentData = player.getClass().getMethod("kjs$getPersistentData");
            }
            return (CompoundTag) persistentData.invoke(player);
        }
        catch (final ReflectiveOperationException | ClassCastException e)
        {
            if (!missingLogged)
            {
                LegacyColonies.LOGGER.warn("No KubeJS player data, the school cannot read the character: {}", e.toString());
                missingLogged = true;
            }
            return new CompoundTag();
        }
    }

    public static List<String> traits(final CompoundTag data)
    {
        return split(data.getString(TRAITS));
    }

    /**
     * The traits added for the heir.
     */
    public static List<String> added(final CompoundTag data)
    {
        return choices(data, ADD);
    }

    /**
     * The traits the heir will not have.
     */
    public static List<String> dropped(final CompoundTag data)
    {
        return choices(data, DROP);
    }

    public static void setChoices(final CompoundTag data, final List<String> added, final List<String> dropped)
    {
        final List<String> entries = new ArrayList<>();
        added.forEach(trait -> entries.add(ADD + trait));
        dropped.forEach(trait -> entries.add(DROP + trait));
        if (entries.isEmpty())
        {
            data.remove(SCHOOL);
        }
        else
        {
            data.putString(SCHOOL, String.join(",", entries));
        }
    }

    private static List<String> choices(final CompoundTag data, final String prefix)
    {
        final List<String> traits = new ArrayList<>();
        split(data.getString(SCHOOL)).stream().filter(entry -> entry.startsWith(prefix)).forEach(entry -> traits.add(entry.substring(prefix.length())));
        return traits;
    }

    private static List<String> split(final String raw)
    {
        return raw.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(raw.split(",")));
    }
}
