package com.chocodanette.legacycolonies.needs;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.happiness.IHappinessModifier;
import com.minecolonies.api.util.constant.NbtTagConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Happiness of a citizen that went without a ration from the granary: a little unhappy after one day, more after two.
 * Unhappy citizens work more slowly.
 * <p>
 * MineColonies only saves and sends its own modifiers, so this one lives on the server and is added again to each citizen
 * by {@link ColonyRations}. Its state is in {@link ColonyRationData}.
 */
public class RationHappinessModifier implements IHappinessModifier
{
    public static final String ID = "legacycolonies_rations";

    private static final double WEIGHT       = 3.0;
    private static final double ONE_DAY      = 0.75;
    private static final double SEVERAL_DAYS = 0.5;

    public static final RationHappinessModifier INSTANCE = new RationHappinessModifier();

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public double getFactor(@Nullable final ICitizenData data)
    {
        if (data == null || !(data.getColony().getWorld() instanceof final ServerLevel level) || !ColonyNeeds.rules(data.getColony()).food())
        {
            return 1.0;
        }
        final int days = ColonyRationData.get(level).getHungryDays(data.getColony().getID(), data.getId());
        return days <= 0 ? 1.0 : days == 1 ? ONE_DAY : SEVERAL_DAYS;
    }

    @Override
    public double getWeight()
    {
        return WEIGHT;
    }

    @Override
    public void read(@NotNull final HolderLookup.Provider provider, final CompoundTag compoundNBT, final boolean persist)
    {
    }

    @Override
    public void write(@NotNull final HolderLookup.Provider provider, final CompoundTag compoundNBT, final boolean persist)
    {
        compoundNBT.putString(NbtTagConstants.TAG_ID, ID);
    }
}
