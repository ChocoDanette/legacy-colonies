package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.validation.BuildingValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Sent by the "confirm building" button of a hut window: check the building around the hut.
 */
public record ConfirmBuildingPayload(BlockPos hut) implements CustomPacketPayload
{
    /**
     * Farther than this, the player cannot have the hut window open.
     */
    private static final double MAX_DISTANCE_SQ = 64 * 64;

    public static final Type<ConfirmBuildingPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "confirm_building"));

    public static final StreamCodec<FriendlyByteBuf, ConfirmBuildingPayload> STREAM_CODEC =
      StreamCodec.composite(BlockPos.STREAM_CODEC, ConfirmBuildingPayload::hut, ConfirmBuildingPayload::new);

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(final ConfirmBuildingPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (context.player() instanceof final ServerPlayer player
                  && player.blockPosition().distSqr(payload.hut) <= MAX_DISTANCE_SQ
                  && player.serverLevel().isLoaded(payload.hut))
            {
                BuildingValidator.validate(player, player.serverLevel(), payload.hut);
            }
        });
    }
}
