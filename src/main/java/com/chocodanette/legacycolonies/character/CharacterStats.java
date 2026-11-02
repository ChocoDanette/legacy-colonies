package com.chocodanette.legacycolonies.character;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The stats shown in the character window, one file per stat in {@code legacycolonies/character_stats/<stat>.json}. A
 * modpack replaces or adds files to change the list; a stat whose attribute or mod is missing is not shown.
 */
public final class CharacterStats extends SimpleJsonResourceReloadListener
{
    public static final CharacterStats INSTANCE = new CharacterStats();

    /**
     * Where a stat's value comes from.
     */
    public enum Source
    {
        /**
         * An attribute of the player: its value, and how far it is from the attribute's base value.
         */
        ATTRIBUTE,
        /**
         * The slots unlocked by Inventory Free.
         */
        INVENTORY_SLOTS;

        public static final Codec<Source> CODEC = Codec.STRING.comapFlatMap(Source::parse, source -> source.name().toLowerCase(Locale.ROOT));

        private static DataResult<Source> parse(final String name)
        {
            for (final Source source : values())
            {
                if (source.name().equalsIgnoreCase(name))
                {
                    return DataResult.success(source);
                }
            }
            return DataResult.error(() -> "Unknown source " + name + ", expected attribute or inventory_slots");
        }
    }

    /**
     * @param label         name shown in the window: a translation key, or plain text.
     * @param source        where the value comes from.
     * @param attribute     the attribute, for the {@link Source#ATTRIBUTE} source.
     * @param multiplier    shown as a multiplier ("×1.5") instead of a number.
     * @param lowerIsBetter a value under the base is shown in green (e.g. crafting time).
     * @param unit          shown after the value: a translation key, or plain text.
     * @param order         position in the window, smallest first.
     */
    public record Stat(String label, Source source, Optional<ResourceLocation> attribute, boolean multiplier, boolean lowerIsBetter, String unit, int order)
    {
        public static final Codec<Stat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.STRING.fieldOf("label").forGetter(Stat::label),
          Source.CODEC.optionalFieldOf("source", Source.ATTRIBUTE).forGetter(Stat::source),
          ResourceLocation.CODEC.optionalFieldOf("attribute").forGetter(Stat::attribute),
          Codec.BOOL.optionalFieldOf("multiplier", false).forGetter(Stat::multiplier),
          Codec.BOOL.optionalFieldOf("lower_is_better", false).forGetter(Stat::lowerIsBetter),
          Codec.STRING.optionalFieldOf("unit", "").forGetter(Stat::unit),
          Codec.INT.optionalFieldOf("order", 100).forGetter(Stat::order)
        ).apply(instance, Stat::new));
    }

    private List<Stat> stats = List.of();

    private CharacterStats()
    {
        super(new Gson(), LegacyColonies.MOD_ID + "/character_stats");
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> files, final ResourceManager resourceManager, final ProfilerFiller profiler)
    {
        final List<Map.Entry<ResourceLocation, Stat>> loaded = new ArrayList<>();
        files.forEach((file, json) -> Stat.CODEC.parse(JsonOps.INSTANCE, json)
          .resultOrPartial(error -> LegacyColonies.LOGGER.error("Invalid character stat {}: {}", file, error))
          .ifPresent(stat -> loaded.add(Map.entry(file, stat))));
        loaded.sort(Comparator.<Map.Entry<ResourceLocation, Stat>>comparingInt(entry -> entry.getValue().order()).thenComparing(entry -> entry.getKey().toString()));
        stats = loaded.stream().map(Map.Entry::getValue).toList();
        LegacyColonies.LOGGER.info("Loaded {} character stats", stats.size());
    }

    /**
     * Every stat, in the window's order.
     */
    public List<Stat> all()
    {
        return stats;
    }

    @SubscribeEvent
    public static void onAddReloadListener(final AddReloadListenerEvent event)
    {
        event.addListener(INSTANCE);
    }
}
