package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.client.ClientSchool;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Content of the school's heir window, sent when it opens and after each change.
 *
 * @param level   level of the school: how many changes the player may make.
 * @param changes changes already made.
 * @param current the character's traits; chosen = dropped for the heir.
 * @param others  the other traits; chosen = added for the heir.
 */
public record SchoolPlanPayload(BlockPos hut, int level, int changes, String firstName, boolean married,
                                List<TraitRow> current, List<TraitRow> others) implements CustomPacketPayload
{
    /**
     * @param available whether its button does something now (choose, or cancel the choice).
     */
    public record TraitRow(String id, String label, String description, boolean chosen, boolean available)
    {
        private void write(final FriendlyByteBuf buf)
        {
            buf.writeUtf(id);
            buf.writeUtf(label);
            buf.writeUtf(description);
            buf.writeBoolean(chosen);
            buf.writeBoolean(available);
        }

        private static TraitRow read(final FriendlyByteBuf buf)
        {
            return new TraitRow(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readBoolean());
        }
    }

    public static final Type<SchoolPlanPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "school_plan"));

    public static final StreamCodec<FriendlyByteBuf, SchoolPlanPayload> STREAM_CODEC =
      StreamCodec.of(SchoolPlanPayload::write, SchoolPlanPayload::read);

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void write(final FriendlyByteBuf buf, final SchoolPlanPayload payload)
    {
        buf.writeBlockPos(payload.hut);
        buf.writeVarInt(payload.level);
        buf.writeVarInt(payload.changes);
        buf.writeUtf(payload.firstName);
        buf.writeBoolean(payload.married);
        writeRows(buf, payload.current);
        writeRows(buf, payload.others);
    }

    private static SchoolPlanPayload read(final FriendlyByteBuf buf)
    {
        return new SchoolPlanPayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readBoolean(),
          readRows(buf), readRows(buf));
    }

    private static void writeRows(final FriendlyByteBuf buf, final List<TraitRow> rows)
    {
        buf.writeVarInt(rows.size());
        rows.forEach(row -> row.write(buf));
    }

    private static List<TraitRow> readRows(final FriendlyByteBuf buf)
    {
        final int count = buf.readVarInt();
        final List<TraitRow> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            rows.add(TraitRow.read(buf));
        }
        return rows;
    }

    public static void handle(final SchoolPlanPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> ClientSchool.show(payload));
    }
}
