package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.material.ClientMaterialChoices;
import com.chocodanette.legacycolonies.material.MaterialChoices;
import com.chocodanette.legacycolonies.material.MaterialSlot;
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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends the material choices of a level to its players, so the build window can list the right materials.
 */
public record MaterialChoicesPayload(Map<BlockPos, EnumMap<MaterialSlot, ResourceLocation>> choices) implements CustomPacketPayload
{
    public static final Type<MaterialChoicesPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "material_choices"));

    public static final StreamCodec<FriendlyByteBuf, MaterialChoicesPayload> STREAM_CODEC =
      StreamCodec.of(MaterialChoicesPayload::write, MaterialChoicesPayload::read);

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void write(final FriendlyByteBuf buf, final MaterialChoicesPayload payload)
    {
        buf.writeVarInt(payload.choices.size());
        payload.choices.forEach((pos, materials) -> {
            buf.writeBlockPos(pos);
            buf.writeVarInt(materials.size());
            materials.forEach((slot, material) -> {
                buf.writeEnum(slot);
                buf.writeResourceLocation(material);
            });
        });
    }

    private static MaterialChoicesPayload read(final FriendlyByteBuf buf)
    {
        final Map<BlockPos, EnumMap<MaterialSlot, ResourceLocation>> choices = new HashMap<>();
        final int buildings = buf.readVarInt();
        for (int i = 0; i < buildings; i++)
        {
            final BlockPos pos = buf.readBlockPos();
            final EnumMap<MaterialSlot, ResourceLocation> materials = new EnumMap<>(MaterialSlot.class);
            final int count = buf.readVarInt();
            for (int j = 0; j < count; j++)
            {
                materials.put(buf.readEnum(MaterialSlot.class), buf.readResourceLocation());
            }
            choices.put(pos, materials);
        }
        return new MaterialChoicesPayload(choices);
    }

    public static void handle(final MaterialChoicesPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> ClientMaterialChoices.set(payload.choices));
    }

    /**
     * A copy of the choices: in single player the payload reaches the client without being encoded.
     */
    private static MaterialChoicesPayload snapshot(final MaterialChoices data)
    {
        final Map<BlockPos, EnumMap<MaterialSlot, ResourceLocation>> copy = new HashMap<>();
        data.getAll().forEach((pos, materials) -> copy.put(pos, new EnumMap<>(materials)));
        return new MaterialChoicesPayload(copy);
    }

    public static void sendTo(final ServerPlayer player)
    {
        PacketDistributor.sendToPlayer(player, snapshot(MaterialChoices.get(player.serverLevel())));
    }

    public static void sendToLevel(final ServerLevel level)
    {
        PacketDistributor.sendToPlayersInDimension(level, snapshot(MaterialChoices.get(level)));
    }
}
