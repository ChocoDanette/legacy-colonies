package com.chocodanette.legacycolonies.validation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Blocks a hand-built building needs to reach each level of a hut, read from {@code data/<namespace>/legacycolonies/building_rules/}.
 *
 * @param hut    registry name of the MineColonies building, for example {@code legacycolonies:inn}.
 * @param levels rules per level, keyed by the level number.
 */
public record BuildingRules(ResourceLocation hut, Map<String, LevelRules> levels)
{
    public static final Codec<BuildingRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      ResourceLocation.CODEC.fieldOf("hut").forGetter(BuildingRules::hut),
      Codec.unboundedMap(Codec.STRING, LevelRules.CODEC).fieldOf("levels").forGetter(BuildingRules::levels)
    ).apply(instance, BuildingRules::new));

    public Optional<LevelRules> level(final int level)
    {
        return Optional.ofNullable(levels.get(Integer.toString(level)));
    }

    /**
     * @param maxVolume largest number of open blocks the room may hold; a bigger one is taken as not closed.
     * @param closed    whether the room must be closed on all sides; when false, it only needs a roof (doorways and
     *                  windows without glass are allowed, for the primitive age).
     * @param door      whether the room needs a door.
     * @param light     whether the room needs a light source.
     * @param requires  blocks the room must contain.
     */
    public record LevelRules(int maxVolume, boolean closed, boolean door, boolean light, List<Requirement> requires)
    {
        public static final int DEFAULT_MAX_VOLUME = 1000;

        public static final Codec<LevelRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.intRange(1, 32768).optionalFieldOf("max_volume", DEFAULT_MAX_VOLUME).forGetter(LevelRules::maxVolume),
          Codec.BOOL.optionalFieldOf("closed", true).forGetter(LevelRules::closed),
          Codec.BOOL.optionalFieldOf("door", true).forGetter(LevelRules::door),
          Codec.BOOL.optionalFieldOf("light", true).forGetter(LevelRules::light),
          Requirement.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(LevelRules::requires)
        ).apply(instance, LevelRules::new));
    }

    /**
     * @param blocks block ids, or block tags starting with {@code #}.
     * @param count  how many of these blocks the room needs.
     * @param name   name shown to the player; the first block's name when absent.
     */
    public record Requirement(List<String> blocks, int count, Optional<String> name)
    {
        public static final Codec<Requirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.STRING.listOf().fieldOf("blocks").forGetter(Requirement::blocks),
          Codec.intRange(1, 4096).optionalFieldOf("count", 1).forGetter(Requirement::count),
          Codec.STRING.optionalFieldOf("name").forGetter(Requirement::name)
        ).apply(instance, Requirement::new));

        public boolean matches(final BlockState state)
        {
            for (final String entry : blocks)
            {
                if (entry.startsWith("#"))
                {
                    final ResourceLocation id = ResourceLocation.tryParse(entry.substring(1));
                    if (id != null && state.is(TagKey.create(Registries.BLOCK, id)))
                    {
                        return true;
                    }
                }
                else
                {
                    final ResourceLocation id = ResourceLocation.tryParse(entry);
                    if (id != null && BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(id))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        public Component displayName()
        {
            if (name.isPresent())
            {
                return Component.literal(name.get());
            }
            final String first = blocks.isEmpty() ? "?" : blocks.get(0);
            final ResourceLocation id = ResourceLocation.tryParse(first);
            if (id != null && !first.startsWith("#") && BuiltInRegistries.BLOCK.containsKey(id))
            {
                final Block block = BuiltInRegistries.BLOCK.get(id);
                return block.getName();
            }
            return Component.literal(first);
        }
    }
}
