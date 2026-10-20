package com.chocodanette.legacycolonies.material;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The material choices of the player's current level, as last sent by the server.
 */
public final class ClientMaterialChoices
{
    private static volatile Map<BlockPos, EnumMap<MaterialSlot, ResourceLocation>> choices = Map.of();

    private ClientMaterialChoices()
    {
    }

    public static void set(final Map<BlockPos, EnumMap<MaterialSlot, ResourceLocation>> newChoices)
    {
        choices = newChoices;
    }

    @Nullable
    public static Map<MaterialSlot, ResourceLocation> getChoices(final BlockPos building)
    {
        return choices.get(building);
    }
}
