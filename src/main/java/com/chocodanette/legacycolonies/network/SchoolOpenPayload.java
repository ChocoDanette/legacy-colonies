package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.school.HeirSchool;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Sent by the "heir" button of the school's window: asks for the heir window's content.
 */
public record SchoolOpenPayload(BlockPos hut) implements CustomPacketPayload
{
    public static final Type<SchoolOpenPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "school_open"));

    public static final StreamCodec<FriendlyByteBuf, SchoolOpenPayload> STREAM_CODEC =
      StreamCodec.composite(BlockPos.STREAM_CODEC, SchoolOpenPayload::hut, SchoolOpenPayload::new);

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(final SchoolOpenPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (context.player() instanceof final ServerPlayer player)
            {
                HeirSchool.open(player, payload.hut);
            }
        });
    }
}
