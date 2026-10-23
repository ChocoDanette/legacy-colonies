package com.chocodanette.legacycolonies.material;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A material the player can choose per building. Plans are drawn with the reference material; every block of its
 * family is replaced by the same block of the chosen material when the builder receives the plan.
 * <p>
 * TFC names families as {@code <namespace>:<family prefix><material>[_<shape>]}, e.g. {@code tfc:wood/planks/ash_stairs}.
 */
public enum MaterialSlot
{
    WOOD("wood", "wood/", "wood/log/", "ash"),
    MUD_BRICK("mud_brick", "mud_bricks/", "mud_bricks/", "entisol"),
    ROCK_BRICK("rock_brick", "rock/bricks/", "rock/bricks/", "andesite");

    private final String id;
    private final String familyPrefix;
    private final String choicePrefix;
    private final String reference;

    MaterialSlot(final String id, final String familyPrefix, final String choicePrefix, final String reference)
    {
        this.id = id;
        this.familyPrefix = familyPrefix;
        this.choicePrefix = choicePrefix;
        this.reference = reference;
    }

    public String getId()
    {
        return id;
    }

    /**
     * The material named by a block the player holds to choose it (a log for wood, a mud brick block for mud bricks, a rock brick block for rock bricks).
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
        if (!path.startsWith(familyPrefix))
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
