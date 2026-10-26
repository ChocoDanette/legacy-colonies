package com.chocodanette.legacycolonies.needs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * What a colony needs, and may hold, during one era of the modpack, read from {@code data/<namespace>/legacycolonies/colony_eras/}.
 *
 * @param era          number of the era: 1 primitive, 2 copper, 3 iron, 4 steel, 5 industrial, 6 space.
 * @param maxCitizens  most citizens the colony may have.
 * @param beds         whether each citizen needs a bed: new citizens then only arrive while a bed is free.
 * @param arrivalDays  game days between two new citizens.
 * @param huts         limits per hut, keyed by the registry name of the MineColonies building.
 */
public record EraRules(int era, int maxCitizens, boolean beds, double arrivalDays, Map<ResourceLocation, HutLimits> huts)
{
    public static final EraRules NONE = new EraRules(0, Integer.MAX_VALUE, false, 2, Map.of());

    public static final Codec<EraRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.intRange(1, 100).fieldOf("era").forGetter(EraRules::era),
      Codec.intRange(1, 500).fieldOf("max_citizens").forGetter(EraRules::maxCitizens),
      Codec.BOOL.optionalFieldOf("beds", false).forGetter(EraRules::beds),
      Codec.doubleRange(0.01, 100).optionalFieldOf("arrival_days", 2.0).forGetter(EraRules::arrivalDays),
      Codec.unboundedMap(ResourceLocation.CODEC, HutLimits.CODEC).optionalFieldOf("huts", Map.of()).forGetter(EraRules::huts)
    ).apply(instance, EraRules::new));

    /**
     * @param maxCount how many of this hut may be built (level 1 or more).
     * @param maxLevel highest level the hut may reach.
     */
    public record HutLimits(int maxCount, int maxLevel)
    {
        public static final int NO_LIMIT = Integer.MAX_VALUE;

        public static final Codec<HutLimits> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.intRange(0, 1000).optionalFieldOf("max_count", NO_LIMIT).forGetter(HutLimits::maxCount),
          Codec.intRange(0, 100).optionalFieldOf("max_level", NO_LIMIT).forGetter(HutLimits::maxLevel)
        ).apply(instance, HutLimits::new));
    }
}
