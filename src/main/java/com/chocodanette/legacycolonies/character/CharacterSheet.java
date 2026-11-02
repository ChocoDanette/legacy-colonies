package com.chocodanette.legacycolonies.character;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.network.CharacterSheetPayload;
import com.chocodanette.legacycolonies.network.CharacterSheetPayload.StatRow;
import com.chocodanette.legacycolonies.network.CharacterSheetPayload.TraitRow;
import com.chocodanette.legacycolonies.school.CharacterData;
import com.chocodanette.legacycolonies.school.TraitCatalog;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The character window: who the character is (from the modpack's KubeJS data, see {@link CharacterData}), their traits,
 * their stats (read on the player, see {@link CharacterStats}) and what the school planned for their heir.
 */
public final class CharacterSheet
{
    private static final String INVENTORY_FREE = "kirderf1.inventoryfree.PlayerData";

    private static Method unlockedSlots;
    private static boolean inventoryFreeMissing;

    private CharacterSheet()
    {
    }

    public static void open(final ServerPlayer player)
    {
        PacketDistributor.sendToPlayer(player, sheet(player));
    }

    private static CharacterSheetPayload sheet(final ServerPlayer player)
    {
        final CompoundTag data = CharacterData.of(player);
        final String fullName = (data.getString(CharacterData.FIRST_NAME) + " " + data.getString(CharacterData.LAST_NAME)).trim();
        final IColony colony = IColonyManager.getInstance().getIColonyByOwner(player.level(), player);
        final List<TraitRow> traits = new ArrayList<>();
        CharacterData.traits(data).forEach(id -> traits.add(trait(id)));
        final List<String> added = new ArrayList<>();
        CharacterData.added(data).forEach(id -> added.add(trait(id).label()));
        final List<String> dropped = new ArrayList<>();
        CharacterData.dropped(data).forEach(id -> dropped.add(trait(id).label()));
        return new CharacterSheetPayload(fullName, Math.max(1, data.getInt(CharacterData.GENERATION)), data.getString(CharacterData.SPOUSE),
          colony == null ? "" : colony.getName(), traits, stats(player), added, dropped);
    }

    private static TraitRow trait(final String id)
    {
        final TraitCatalog.Trait trait = TraitCatalog.INSTANCE.get(id);
        return new TraitRow(trait == null ? id : trait.label(), trait == null ? "" : trait.description());
    }

    private static List<StatRow> stats(final ServerPlayer player)
    {
        final List<StatRow> rows = new ArrayList<>();
        for (final CharacterStats.Stat stat : CharacterStats.INSTANCE.all())
        {
            final StatRow row = switch (stat.source())
            {
                case ATTRIBUTE -> attributeRow(player, stat);
                case INVENTORY_SLOTS -> slotsRow(player, stat);
            };
            if (row != null)
            {
                rows.add(row);
            }
        }
        return rows;
    }

    @Nullable
    private static StatRow attributeRow(final Player player, final CharacterStats.Stat stat)
    {
        final Optional<Holder.Reference<Attribute>> attribute = stat.attribute().flatMap(BuiltInRegistries.ATTRIBUTE::getHolder);
        final AttributeInstance instance = attribute.map(player::getAttribute).orElse(null);
        if (instance == null)
        {
            return null;
        }
        return new StatRow(stat.label(), instance.getValue(), instance.getBaseValue(), true, stat.multiplier(), stat.lowerIsBetter(), stat.unit());
    }

    /**
     * The slots Inventory Free unlocked, read through reflection: the mod is not a dependency of the addon.
     */
    @Nullable
    private static StatRow slotsRow(final Player player, final CharacterStats.Stat stat)
    {
        if (inventoryFreeMissing)
        {
            return null;
        }
        try
        {
            if (unlockedSlots == null)
            {
                unlockedSlots = findUnlockedSlots(player);
            }
            final int slots = ((Number) unlockedSlots.invoke(null, player)).intValue();
            return new StatRow(stat.label(), slots, slots, false, false, false, stat.unit());
        }
        catch (final ReflectiveOperationException | ClassCastException | LinkageError e)
        {
            LegacyColonies.LOGGER.info("No Inventory Free slots for the character window: {}", e.toString());
            inventoryFreeMissing = true;
            return null;
        }
    }

    /**
     * {@code PlayerData.getUnlockedSlots(<player>)}, whatever player class its parameter is.
     */
    private static Method findUnlockedSlots(final Player player) throws ReflectiveOperationException
    {
        for (final Method method : Class.forName(INVENTORY_FREE).getMethods())
        {
            if (method.getName().equals("getUnlockedSlots") && method.getParameterCount() == 1 && method.getParameterTypes()[0].isInstance(player))
            {
                return method;
            }
        }
        throw new NoSuchMethodException(INVENTORY_FREE + ".getUnlockedSlots");
    }
}
