package com.chocodanette.legacycolonies.school;

import com.chocodanette.legacycolonies.ModBuildings;
import com.chocodanette.legacycolonies.network.SchoolPlanPayload;
import com.chocodanette.legacycolonies.network.SchoolPlanPayload.TraitRow;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The school's heir window: each level of the school lets the player make one change to the traits of their heir, add a
 * trait or drop one of theirs. The heir never has more than {@link #MAX_TRAITS} traits, so a full character drops one
 * before adding one. The modpack's KubeJS script applies the changes when a married character dies, so only a married character can make new ones.
 */
public final class HeirSchool
{
    public static final int MAX_TRAITS = 3;

    /**
     * Farther than this, the player cannot have the hut window open.
     */
    private static final double MAX_DISTANCE_SQ = 64 * 64;

    public enum Change
    {
        ADD,
        DROP,
        CANCEL
    }

    private HeirSchool()
    {
    }

    /**
     * Sends the window's content, which opens it on the client or refreshes it.
     */
    public static void open(final ServerPlayer player, final BlockPos hut)
    {
        final IBuilding school = school(player, hut);
        if (school != null)
        {
            PacketDistributor.sendToPlayer(player, plan(player, hut, school.getBuildingLevel()));
        }
    }

    public static void change(final ServerPlayer player, final BlockPos hut, final Change change, final String trait)
    {
        final IBuilding school = school(player, hut);
        if (school == null)
        {
            return;
        }
        final int level = school.getBuildingLevel();
        final CompoundTag data = CharacterData.of(player);
        final List<String> current = CharacterData.traits(data);
        final List<String> added = CharacterData.added(data);
        final List<String> dropped = CharacterData.dropped(data);
        final boolean married = data.contains(CharacterData.SPOUSE);
        final boolean allowed = switch (change)
        {
            case ADD -> married && canAdd(trait, level, current, added, dropped);
            case DROP -> married && canDrop(trait, level, current, added, dropped);
            case CANCEL -> added.contains(trait) || dropped.contains(trait) && canRestore(trait, current, added, dropped);
        };
        if (allowed)
        {
            switch (change)
            {
                case ADD -> added.add(trait);
                case DROP -> dropped.add(trait);
                case CANCEL ->
                {
                    added.remove(trait);
                    dropped.remove(trait);
                }
            }
            CharacterData.setChoices(data, added, dropped);
        }
        PacketDistributor.sendToPlayer(player, plan(player, hut, level));
    }

    /**
     * The school at this position, if the player may use it.
     */
    @Nullable
    private static IBuilding school(final ServerPlayer player, final BlockPos hut)
    {
        if (player.blockPosition().distSqr(hut) > MAX_DISTANCE_SQ || !player.serverLevel().isLoaded(hut))
        {
            return null;
        }
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(player.serverLevel(), hut);
        final IBuilding building = IColonyManager.getInstance().getBuilding(player.serverLevel(), hut);
        if (colony == null || building == null || building.getBuildingType() != ModBuildings.SCHOOL.get() || building.getBuildingLevel() < 1
              || !colony.getPermissions().hasPermission(player, Action.ACCESS_HUTS))
        {
            return null;
        }
        return building;
    }

    private static SchoolPlanPayload plan(final ServerPlayer player, final BlockPos hut, final int level)
    {
        final CompoundTag data = CharacterData.of(player);
        final List<String> current = CharacterData.traits(data);
        final List<String> added = CharacterData.added(data);
        final List<String> dropped = CharacterData.dropped(data);
        // Only a married character has an heir, so an unmarried one can only cancel earlier choices.
        final boolean married = data.contains(CharacterData.SPOUSE);

        final List<TraitRow> currentRows = new ArrayList<>();
        for (final String trait : current)
        {
            final boolean chosen = dropped.contains(trait);
            final boolean available = chosen ? canRestore(trait, current, added, dropped) : married && canDrop(trait, level, current, added, dropped);
            currentRows.add(row(trait, chosen, available));
        }
        final List<TraitRow> otherRows = new ArrayList<>();
        for (final String trait : TraitCatalog.INSTANCE.all().keySet())
        {
            if (!current.contains(trait))
            {
                final boolean chosen = added.contains(trait);
                otherRows.add(row(trait, chosen, chosen || married && canAdd(trait, level, current, added, dropped)));
            }
        }
        return new SchoolPlanPayload(hut, level, added.size() + dropped.size(), data.getString(CharacterData.FIRST_NAME),
          data.contains(CharacterData.SPOUSE), currentRows, otherRows);
    }

    private static TraitRow row(final String id, final boolean chosen, final boolean available)
    {
        final TraitCatalog.Trait trait = TraitCatalog.INSTANCE.get(id);
        return new TraitRow(id, trait == null ? id : trait.label(), trait == null ? "" : trait.description(), chosen, available);
    }

    /**
     * The heir's traits if nothing else changed: the character's, without the dropped ones, with the added ones.
     */
    private static List<String> planned(final List<String> current, final List<String> added, final List<String> dropped)
    {
        final List<String> planned = new ArrayList<>(current);
        planned.removeAll(dropped);
        planned.addAll(added);
        return planned;
    }

    private static boolean canAdd(final String trait, final int level, final List<String> current, final List<String> added, final List<String> dropped)
    {
        final List<String> planned = planned(current, added, dropped);
        return TraitCatalog.INSTANCE.get(trait) != null && !current.contains(trait) && !added.contains(trait)
                 && added.size() + dropped.size() < level && planned.size() < MAX_TRAITS
                 && planned.stream().noneMatch(other -> TraitCatalog.INSTANCE.opposite(trait, other));
    }

    private static boolean canDrop(final String trait, final int level, final List<String> current, final List<String> added, final List<String> dropped)
    {
        return current.contains(trait) && !dropped.contains(trait) && added.size() + dropped.size() < level;
    }

    /**
     * Whether a dropped trait can be kept again: there must be room for it, and no added trait may be its opposite.
     */
    private static boolean canRestore(final String trait, final List<String> current, final List<String> added, final List<String> dropped)
    {
        final List<String> planned = planned(current, added, dropped);
        return planned.size() < MAX_TRAITS && added.stream().noneMatch(other -> TraitCatalog.INSTANCE.opposite(trait, other));
    }
}
