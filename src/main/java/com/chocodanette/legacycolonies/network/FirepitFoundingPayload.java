package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.client.ClientFounding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Sent when a TFC firepit window opens and the player may found a colony there: shows the "found a colony here" button.
 */
public record FirepitFoundingPayload(int containerId, BlockPos firepit) implements CustomPacketPayload
{
    public static final Type<FirepitFoundingPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "firepit_founding"));

    public static final StreamCodec<FriendlyByteBuf, FirepitFoundingPayload> STREAM_CODEC =
      StreamCodec.composite(ByteBufCodecs.VAR_INT, FirepitFoundingPayload::containerId, BlockPos.STREAM_CODEC, FirepitFoundingPayload::firepit,
        FirepitFoundingPayload::new);

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(final FirepitFoundingPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> ClientFounding.allow(payload));
    }
}
