package com.chocodanette.legacycolonies.needs;

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

import java.util.Map;
import java.util.TreeMap;

/**
 * Loads the rules of each era from every datapack, one file per era.
 */
public final class EraRulesManager extends SimpleJsonResourceReloadListener
{
    public static final EraRulesManager INSTANCE = new EraRulesManager();

    private TreeMap<Integer, EraRules> eras = new TreeMap<>();

    private EraRulesManager()
    {
        super(new Gson(), LegacyColonies.MOD_ID + "/colony_eras");
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> files, final ResourceManager resourceManager, final ProfilerFiller profiler)
    {
        final TreeMap<Integer, EraRules> loaded = new TreeMap<>();
        files.forEach((file, json) -> EraRules.CODEC.parse(JsonOps.INSTANCE, json)
          .resultOrPartial(error -> LegacyColonies.LOGGER.error("Invalid colony era {}: {}", file, error))
          .ifPresent(parsed -> loaded.put(parsed.era(), parsed)));
        eras = loaded;
        LegacyColonies.LOGGER.info("Loaded rules for {} colony eras", loaded.size());
    }

    /**
     * Rules of an era. An era without its own file keeps the rules of the last era before it.
     */
    public EraRules get(final int era)
    {
        final Map.Entry<Integer, EraRules> entry = eras.floorEntry(era);
        if (entry != null)
        {
            return entry.getValue();
        }
        return eras.isEmpty() ? EraRules.NONE : eras.firstEntry().getValue();
    }

    @SubscribeEvent
    public static void onAddReloadListener(final AddReloadListenerEvent event)
    {
        event.addListener(INSTANCE);
    }
}
