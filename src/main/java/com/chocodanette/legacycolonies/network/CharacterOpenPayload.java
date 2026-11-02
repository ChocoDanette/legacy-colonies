package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.character.CharacterSheet;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Sent by the character key or the inventory's "Character" button: asks for the character window's content.
 */
public record CharacterOpenPayload() implements CustomPacketPayload
{
    public static final Type<CharacterOpenPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "character_open"));

    public static final StreamCodec<ByteBuf, CharacterOpenPayload> STREAM_CODEC = StreamCodec.unit(new CharacterOpenPayload());

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(final CharacterOpenPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (context.player() instanceof final ServerPlayer player)
            {
                CharacterSheet.open(player);
            }
        });
    }
}
