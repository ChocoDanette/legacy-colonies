package com.chocodanette.legacycolonies.needs;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.building.module.LegacyLivingModule;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.util.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Needs of a colony by era: how many citizens it may have, whether they need beds, and how many huts of each kind, and
 * of which level, it may build.
 * <p>
 * The era comes from the modpack: its colony script calls {@link #setColonyEra} with the era of the colony's managers.
 */
public final class ColonyNeeds
{
    private static final String KEY = "com.legacycolonies.needs.";

    private ColonyNeeds()
    {
    }

    /**
     * Called by the modpack (KubeJS). Eras only move forward.
     */
    public static void setColonyEra(final IColony colony, final int era)
    {
        if (colony.getWorld() instanceof final ServerLevel level && ColonyEras.get(level).raiseEra(colony.getID(), era))
        {
            LegacyColonies.LOGGER.info("Colony {} enters era {}", colony.getID(), era);
            MessageUtils.format(Component.translatable(KEY + "era_changed", eraName(era), rules(colony).maxCitizens())).sendTo(colony).forAllPlayers();
        }
    }

    public static int getEra(final IColony colony)
    {
        return colony.getWorld() instanceof final ServerLevel level ? ColonyEras.get(level).getEra(colony.getID()) : 1;
    }

    public static EraRules rules(final IColony colony)
    {
        return EraRulesManager.INSTANCE.get(getEra(colony));
    }

    public static Component eraName(final int era)
    {
        return Component.translatable("com.legacycolonies.era." + era);
    }

    /**
     * Beds the colony's Legacy houses and inn offer to its citizens.
     */
    public static int beds(final IColony colony)
    {
        int beds = 0;
        for (final IBuilding building : colony.getServerBuildingManager().getBuildings().values())
        {
            final LegacyLivingModule living = building.getModule(LegacyLivingModule.class);
            if (living != null)
            {
                beds += living.getModuleMax();
            }
        }
        return beds;
    }

    /**
     * How many citizens the colony may have now: the era's maximum, and no more than its beds when the era needs beds.
     */
    public static int allowedCitizens(final IColony colony)
    {
        final EraRules rules = rules(colony);
        return rules.beds() ? Math.min(rules.maxCitizens(), beds(colony)) : rules.maxCitizens();
    }

    public static EraRules.HutLimits limits(final IColony colony, final BuildingEntry type)
    {
        final EraRules.HutLimits limits = rules(colony).huts().get(type.getRegistryName());
        return limits == null ? new EraRules.HutLimits(EraRules.HutLimits.NO_LIMIT, EraRules.HutLimits.NO_LIMIT) : limits;
    }

    /**
     * Huts of this kind in the colony.
     *
     * @param built  count only huts of level 1 or more.
     * @param except a hut not to count, or null.
     */
    public static int count(final IColony colony, final BuildingEntry type, final boolean built, @Nullable final BlockPos except)
    {
        final ResourceLocation name = type.getRegistryName();
        int count = 0;
        for (final IBuilding building : colony.getServerBuildingManager().getBuildings().values())
        {
            if (building.getBuildingType().getRegistryName().equals(name)
                  && (!built || building.getBuildingLevel() > 0)
                  && !building.getPosition().equals(except))
            {
                count++;
            }
        }
        return count;
    }

    /**
     * @return why a hut of this kind cannot be placed in the colony during its era, or null when it can.
     */
    @Nullable
    public static Component refusePlacement(final IColony colony, final BuildingEntry type)
    {
        final EraRules.HutLimits limits = limits(colony, type);
        if (limits.maxCount() != EraRules.HutLimits.NO_LIMIT && count(colony, type, false, null) >= limits.maxCount())
        {
            return Component.translatable(KEY + "too_many", eraName(getEra(colony)), limits.maxCount(), hutName(type));
        }
        return null;
    }

    /**
     * @return why the building cannot reach this level during the colony's era, or null when it can.
     */
    @Nullable
    public static Component refuseLevel(final IBuilding building, final int target)
    {
        final IColony colony = building.getColony();
        final BuildingEntry type = building.getBuildingType();
        final EraRules.HutLimits limits = limits(colony, type);
        if (target > limits.maxLevel())
        {
            return Component.translatable(KEY + "level_too_high", hutName(type), target, eraName(getEra(colony)), limits.maxLevel());
        }
        if (target == 1 && limits.maxCount() != EraRules.HutLimits.NO_LIMIT && count(colony, type, true, building.getPosition()) >= limits.maxCount())
        {
            return Component.translatable(KEY + "too_many", eraName(getEra(colony)), limits.maxCount(), hutName(type));
        }
        return null;
    }

    private static Component hutName(final BuildingEntry type)
    {
        return Component.translatable(type.getTranslationKey());
    }
}
