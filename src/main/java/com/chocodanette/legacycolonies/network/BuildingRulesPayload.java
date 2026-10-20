package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.validation.BuildingRulesManager;
import com.chocodanette.legacycolonies.validation.ClientBuildingRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
      ByteBufCodecs.<FriendlyByteBuf, String>collection(HashSet::new, ByteBufCodecs.STRING_UTF8)
        .map(BuildingRulesPayload::new, BuildingRulesPayload::levels);

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
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
