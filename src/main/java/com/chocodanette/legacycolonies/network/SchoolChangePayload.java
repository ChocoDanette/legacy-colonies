package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.school.HeirSchool;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Sent by a button of the heir window: add a trait for the heir, drop one, or cancel that choice.
 */
public record SchoolChangePayload(BlockPos hut, HeirSchool.Change change, String trait) implements CustomPacketPayload
{
    public static final Type<SchoolChangePayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "school_change"));

    public static final StreamCodec<FriendlyByteBuf, SchoolChangePayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, SchoolChangePayload::hut,
      NeoForgeStreamCodecs.enumCodec(HeirSchool.Change.class), SchoolChangePayload::change,
      ByteBufCodecs.STRING_UTF8, SchoolChangePayload::trait,
      SchoolChangePayload::new);

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(final SchoolChangePayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (context.player() instanceof final ServerPlayer player)
            {
                HeirSchool.change(player, payload.hut, payload.change, payload.trait);
            }
        });
    }
}
