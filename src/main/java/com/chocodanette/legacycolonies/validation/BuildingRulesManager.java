package com.chocodanette.legacycolonies.validation;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads the building rules of every datapack, one file per hut.
 */
public final class BuildingRulesManager extends SimpleJsonResourceReloadListener
{
    public static final BuildingRulesManager INSTANCE = new BuildingRulesManager();

    private Map<ResourceLocation, BuildingRules> rules = Map.of();

    private BuildingRulesManager()
    {
        super(new Gson(), LegacyColonies.MOD_ID + "/building_rules");
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> files, final ResourceManager resourceManager, final ProfilerFiller profiler)
    {
        final Map<ResourceLocation, BuildingRules> loaded = new HashMap<>();
        files.forEach((file, json) -> BuildingRules.CODEC.parse(JsonOps.INSTANCE, json)
          .resultOrPartial(error -> LegacyColonies.LOGGER.error("Invalid building rules {}: {}", file, error))
          .ifPresent(parsed -> loaded.put(parsed.hut(), parsed)));
        rules = loaded;
        LegacyColonies.LOGGER.info("Loaded building rules for {} huts", loaded.size());
    }

    @Nullable
    public BuildingRules get(final ResourceLocation hut)
    {
        return rules.get(hut);
    }

    /**
     * Every hut level that has rules, as {@link #key} strings.
     */
    public Set<String> confirmableLevels()
    {
        final Set<String> levels = new HashSet<>();
        rules.values().forEach(hut -> hut.levels().keySet().forEach(level -> levels.add(hut.hut() + "#" + level)));
        return levels;
    }

    public static String key(final ResourceLocation building, final int level)
    {
        return building + "#" + level;
    }

    @SubscribeEvent
    public static void onAddReloadListener(final AddReloadListenerEvent event)
    {
        event.addListener(INSTANCE);
    }
}
