package com.chocodanette.legacycolonies.network;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.client.ClientCharacter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Content of the character window.
 *
 * @param fullName   first and last name, empty before the character is made.
 * @param spouse     name of the spouse, empty when unmarried.
 * @param colony     name of the colony the player owns, empty without one.
 * @param added      labels of the traits the school adds for the heir.
 * @param dropped    labels of the traits the heir will not have.
 */
public record CharacterSheetPayload(String fullName, int generation, String spouse, String colony, List<TraitRow> traits,
                                    List<StatRow> stats, List<String> added, List<String> dropped) implements CustomPacketPayload
{
    public record TraitRow(String label, String description)
    {
        private void write(final FriendlyByteBuf buf)
        {
            buf.writeUtf(label);
            buf.writeUtf(description);
        }

        private static TraitRow read(final FriendlyByteBuf buf)
        {
            return new TraitRow(buf.readUtf(), buf.readUtf());
        }
    }

    /**
     * @param label     translation key or plain text.
     * @param base      the value of an ordinary character.
     * @param showDelta whether to show how far the value is from the base.
     * @param unit      translation key or plain text, may be empty.
     */
    public record StatRow(String label, double value, double base, boolean showDelta, boolean multiplier, boolean lowerIsBetter, String unit)
    {
        private void write(final FriendlyByteBuf buf)
        {
            buf.writeUtf(label);
            buf.writeDouble(value);
            buf.writeDouble(base);
            buf.writeBoolean(showDelta);
            buf.writeBoolean(multiplier);
            buf.writeBoolean(lowerIsBetter);
            buf.writeUtf(unit);
        }

        private static StatRow read(final FriendlyByteBuf buf)
        {
            return new StatRow(buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readUtf());
        }
    }

    public static final Type<CharacterSheetPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "character_sheet"));

    public static final StreamCodec<FriendlyByteBuf, CharacterSheetPayload> STREAM_CODEC =
      StreamCodec.of(CharacterSheetPayload::write, CharacterSheetPayload::read);

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void write(final FriendlyByteBuf buf, final CharacterSheetPayload payload)
    {
        buf.writeUtf(payload.fullName);
        buf.writeVarInt(payload.generation);
        buf.writeUtf(payload.spouse);
        buf.writeUtf(payload.colony);
        writeList(buf, payload.traits, (b, row) -> row.write(b));
        writeList(buf, payload.stats, (b, row) -> row.write(b));
        writeList(buf, payload.added, FriendlyByteBuf::writeUtf);
        writeList(buf, payload.dropped, FriendlyByteBuf::writeUtf);
    }

    private static CharacterSheetPayload read(final FriendlyByteBuf buf)
    {
        return new CharacterSheetPayload(buf.readUtf(), buf.readVarInt(), buf.readUtf(), buf.readUtf(),
          readList(buf, TraitRow::read), readList(buf, StatRow::read), readList(buf, FriendlyByteBuf::readUtf), readList(buf, FriendlyByteBuf::readUtf));
    }

    private static <T> void writeList(final FriendlyByteBuf buf, final List<T> list, final BiConsumer<FriendlyByteBuf, T> writer)
    {
        buf.writeVarInt(list.size());
        list.forEach(element -> writer.accept(buf, element));
    }

    private static <T> List<T> readList(final FriendlyByteBuf buf, final Function<FriendlyByteBuf, T> reader)
    {
        final int count = buf.readVarInt();
        final List<T> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            list.add(reader.apply(buf));
        }
        return list;
    }

    public static void handle(final CharacterSheetPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> ClientCharacter.show(payload));
    }
}
