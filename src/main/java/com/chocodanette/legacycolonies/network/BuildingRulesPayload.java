package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.validation.BuildingRulesManager;
import com.chocodanette.legacycolonies.validation.ClientBuildingRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Tells the client which hut levels can be confirmed by hand, so the hut window shows the "confirm building" button.
 * Each entry is {@code <building registry name>#<level>}.
 */
public record BuildingRulesPayload(Set<String> levels) implements CustomPacketPayload
{
    public static final Type<BuildingRulesPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "building_rules"));

    public static final StreamCodec<FriendlyByteBuf, BuildingRulesPayload> STREAM_CODEC =
      StreamCodec.of(BuildingRulesPayload::write, BuildingRulesPayload::read);

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void write(final FriendlyByteBuf buf, final BuildingRulesPayload payload)
    {
        buf.writeVarInt(payload.levels.size());
        payload.levels.forEach(buf::writeUtf);
    }

    private static BuildingRulesPayload read(final FriendlyByteBuf buf)
    {
        final Set<String> levels = new HashSet<>();
        final int count = buf.readVarInt();
        for (int i = 0; i < count; i++)
        {
            levels.add(buf.readUtf());
        }
        return new BuildingRulesPayload(levels);
    }

    public static void handle(final BuildingRulesPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> ClientBuildingRules.set(payload.levels));
    }

    /**
     * Sent when a player joins and after a reload.
     */
    public static void onDatapackSync(final OnDatapackSyncEvent event)
    {
        final BuildingRulesPayload payload = new BuildingRulesPayload(new HashSet<>(BuildingRulesManager.INSTANCE.confirmableLevels()));
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }
}
