package com.chocodanette.legacycolonies.school;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The character traits of the modpack, one file per trait in {@code legacycolonies/traits/<trait>.json}: what the school
 * window shows. Their effects live in the modpack's KubeJS script.
 */
public final class TraitCatalog extends SimpleJsonResourceReloadListener
{
    public static final TraitCatalog INSTANCE = new TraitCatalog();

    /**
     * @param label       name shown in the window.
     * @param description effect shown under the name.
     * @param opposites   traits a character never has together with this one.
     */
    public record Trait(String label, String description, List<String> opposites)
    {
        public static final Codec<Trait> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.STRING.fieldOf("label").forGetter(Trait::label),
          Codec.STRING.optionalFieldOf("description", "").forGetter(Trait::description),
          Codec.STRING.listOf().optionalFieldOf("opposites", List.of()).forGetter(Trait::opposites)
        ).apply(instance, Trait::new));
    }

    private Map<String, Trait> traits = Map.of();

    private TraitCatalog()
    {
        super(new Gson(), LegacyColonies.MOD_ID + "/traits");
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> files, final ResourceManager resourceManager, final ProfilerFiller profiler)
    {
        final Map<String, Trait> loaded = new TreeMap<>();
        files.forEach((file, json) -> Trait.CODEC.parse(JsonOps.INSTANCE, json)
          .resultOrPartial(error -> LegacyColonies.LOGGER.error("Invalid trait {}: {}", file, error))
          .ifPresent(trait -> loaded.put(file.getPath(), trait)));
        traits = loaded;
        LegacyColonies.LOGGER.info("Loaded {} character traits", loaded.size());
    }

    /**
     * Every trait, sorted by id.
     */
    public Map<String, Trait> all()
    {
        return traits;
    }

    @Nullable
    public Trait get(final String id)
    {
        return traits.get(id);
    }

    /**
     * Whether the two traits are opposites, as written in either file.
     */
    public boolean opposite(final String a, final String b)
    {
        final Trait first = traits.get(a);
        final Trait second = traits.get(b);
        return first != null && first.opposites().contains(b) || second != null && second.opposites().contains(a);
    }

    @SubscribeEvent
    public static void onAddReloadListener(final AddReloadListenerEvent event)
    {
        event.addListener(INSTANCE);
    }
}
