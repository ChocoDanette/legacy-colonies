package com.chocodanette.legacycolonies.colony;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The build modes of the player's current level, as last sent by the server.
 */
public final class ClientBuildModes
{
    private static volatile Map<BlockPos, BuildMode> modes = Map.of();

    private ClientBuildModes()
    {
    }

    public static void set(final Map<BlockPos, BuildMode> newModes)
    {
        modes = newModes;
    }

    @Nullable
    public static BuildMode getMode(final BlockPos building)
    {
        return modes.get(building);
    }
}
