package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.colony.HearthConversionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Sent when the player confirms "found a colony here" in a TFC firepit window: the firepit becomes the colony hearth.
 */
public record FoundColonyPayload(BlockPos firepit) implements CustomPacketPayload
{
    public static final Type<FoundColonyPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "found_colony"));

    public static final StreamCodec<FriendlyByteBuf, FoundColonyPayload> STREAM_CODEC =
      StreamCodec.composite(BlockPos.STREAM_CODEC, FoundColonyPayload::firepit, FoundColonyPayload::new);

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(final FoundColonyPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (context.player() instanceof final ServerPlayer player)
            {
                HearthConversionHandler.found(player, payload.firepit);
            }
        });
    }
}
