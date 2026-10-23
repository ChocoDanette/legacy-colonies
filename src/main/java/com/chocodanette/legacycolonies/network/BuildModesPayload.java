package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.colony.BuildMode;
import com.chocodanette.legacycolonies.colony.BuildModes;
import com.chocodanette.legacycolonies.colony.ClientBuildModes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends the build modes of a level to its players, so the hut window shows either the build button or "confirm building".
 */
public record BuildModesPayload(Map<BlockPos, BuildMode> modes) implements CustomPacketPayload
{
    public static final Type<BuildModesPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "build_modes"));

    public static final StreamCodec<FriendlyByteBuf, BuildModesPayload> STREAM_CODEC =
      StreamCodec.of(BuildModesPayload::write, BuildModesPayload::read);

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void write(final FriendlyByteBuf buf, final BuildModesPayload payload)
    {
        buf.writeVarInt(payload.modes.size());
        payload.modes.forEach((pos, mode) -> {
            buf.writeBlockPos(pos);
            buf.writeEnum(mode);
        });
    }

    private static BuildModesPayload read(final FriendlyByteBuf buf)
    {
        final Map<BlockPos, BuildMode> modes = new HashMap<>();
        final int count = buf.readVarInt();
        for (int i = 0; i < count; i++)
        {
            modes.put(buf.readBlockPos(), buf.readEnum(BuildMode.class));
        }
        return new BuildModesPayload(modes);
    }

    public static void handle(final BuildModesPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> ClientBuildModes.set(payload.modes));
    }

    /**
     * A copy of the modes: in single player the payload reaches the client without being encoded.
     */
    private static BuildModesPayload snapshot(final BuildModes data)
    {
        return new BuildModesPayload(new HashMap<>(data.getAll()));
    }

    public static void sendTo(final ServerPlayer player)
    {
        PacketDistributor.sendToPlayer(player, snapshot(BuildModes.get(player.serverLevel())));
    }

    public static void sendToLevel(final ServerLevel level)
    {
        PacketDistributor.sendToPlayersInDimension(level, snapshot(BuildModes.get(level)));
    }
}
