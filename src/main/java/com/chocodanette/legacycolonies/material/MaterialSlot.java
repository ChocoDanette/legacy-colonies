package com.chocodanette.legacycolonies.material;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * A material the player can choose per building. Plans are drawn with the reference material; every block of its
 * family is replaced by the same block of the chosen material when the builder receives the plan.
 * <p>
 * TFC names families as {@code <namespace>:<family prefix><material>[_<shape>]}, e.g. {@code tfc:wood/planks/ash_stairs}.
 */
public enum MaterialSlot
{
    WOOD("wood", "wood/log/", "ash", "wood/"),
    MUD_BRICK("mud_brick", "mud_bricks/", "entisol", "mud_bricks/"),
    ROCK_BRICK("rock_brick", "rock/bricks/", "andesite", "rock/bricks/"),
    /**
     * Rock of the stone anvils; raw rock is in the family too, as the builder requests it to make the anvil.
     */
    ANVIL_ROCK("anvil_rock", "rock/raw/", "dacite", "rock/anvil/", "rock/raw/");

    private final String   id;
    private final String   choicePrefix;
    private final String   reference;
    private final String[] familyPrefixes;

    MaterialSlot(final String id, final String choicePrefix, final String reference, final String... familyPrefixes)
    {
        this.id = id;
        this.choicePrefix = choicePrefix;
        this.reference = reference;
        this.familyPrefixes = familyPrefixes;
    }

    public String getId()
    {
        return id;
    }

    /**
     * The material named by a block the player holds to choose it (a log for wood, a mud brick block for mud bricks, a rock brick block for rock bricks, a raw rock block for stone anvils).
     *
     * @return the chosen material, or null if the block does not choose this slot.
     */
    @Nullable
    public ResourceLocation choiceOf(final ResourceLocation blockId)
    {
        final String path = blockId.getPath();
        if (!path.startsWith(choicePrefix))
        {
            return null;
        }
        final String material = path.substring(choicePrefix.length());
        if (material.isEmpty() || material.contains("/") || material.endsWith("_stairs") || material.endsWith("_slab") || material.endsWith("_wall"))
        {
            return null;
        }
        if (this == ANVIL_ROCK && !BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "rock/anvil/" + material)))
        {
            // Only igneous rocks make anvils in TFC.
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), material);
    }

    /**
     * The block replacing a plan block of the reference material by the chosen material.
     *
     * @return the replacement block id, or null if the block is not of the reference material.
     */
    @Nullable
    public ResourceLocation substitute(final ResourceLocation blockId, final ResourceLocation chosen)
    {
        final String path = blockId.getPath();
        if (Arrays.stream(familyPrefixes).noneMatch(path::startsWith))
        {
            return null;
        }
        final int lastSlash = path.lastIndexOf('/');
        final String last = path.substring(lastSlash + 1);
        if (!last.equals(reference) && !last.startsWith(reference + "_"))
        {
            return null;
        }
        final String newPath = path.substring(0, lastSlash + 1) + chosen.getPath() + last.substring(reference.length());
        return ResourceLocation.fromNamespaceAndPath(chosen.getNamespace(), newPath);
    }
}
