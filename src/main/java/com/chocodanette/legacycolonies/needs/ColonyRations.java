package com.chocodanette.legacycolonies.needs;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.ModBuildings;
import com.chocodanette.legacycolonies.validation.BuildingRules;
import com.chocodanette.legacycolonies.validation.RoomScanner;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenHappinessHandler;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.api.util.constant.CitizenConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Daily rations, from the iron age: at the start of each game day, each citizen takes one food that is not rotten from
 * the food shelves and hangers of the colony's granaries, which must be working Firmalife cellars, and is then full. Chests and
 * barrels are not used. A citizen without a ration gets unhappy (see
 * {@link RationHappinessModifier}), and no new citizen arrives while the granaries could not feed one more.
 */
public final class ColonyRations
{
    private static final int  CHECK_INTERVAL = 100;
    private static final long TICKS_PER_DAY  = 24000;
    private static final String KEY = "com.legacycolonies.needs.food.";

    /**
     * Firmalife's food shelves and hangers, the only stores citizens take their rations from.
     */
    private static final String  FIRMALIFE    = "firmalife";
    private static final Pattern CELLAR_STORE = Pattern.compile("wood/(food_shelf|hanger)/.+");

    private static boolean errorLogged = false;

    /**
     * TFC's {@code FoodCapability.isRotten(ItemStack)}, or null without TFC.
     */
    private static Method  isRotten;
    private static boolean tfcLooked = false;

    /**
     * Firmalife's {@code FoodShelfBlockEntity.isClimateValid()}, or null without Firmalife.
     */
    private static Method  isClimateValid;
    private static boolean firmalifeLooked = false;

    private ColonyRations()
    {
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event)
    {
        if (event.getServer().getTickCount() % CHECK_INTERVAL != 0)
        {
            return;
        }
        for (final IColony colony : IColonyManager.getInstance().getAllColonies())
        {
            try
            {
                tick(colony);
            }
            catch (final RuntimeException e)
            {
                if (!errorLogged)
                {
                    LegacyColonies.LOGGER.error("Colony rations failed for colony {}", colony.getID(), e);
                    errorLogged = true;
                }
            }
        }
    }

    private static void tick(final IColony colony)
    {
        if (!(colony.getWorld() instanceof final ServerLevel level))
        {
            return;
        }
        final ColonyRationData data = ColonyRationData.get(level);
        final long day = level.getDayTime() / TICKS_PER_DAY;
        if (!ColonyNeeds.rules(colony).food())
        {
            data.clearHungry(colony.getID());
            data.setLastDay(colony.getID(), day);
            return;
        }

        // MineColonies forgets modifiers it does not know when it loads a citizen.
        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            final ICitizenHappinessHandler happiness = citizen.getCitizenHappinessHandler();
            if (happiness.getModifier(RationHappinessModifier.ID) == null)
            {
                happiness.addModifier(RationHappinessModifier.INSTANCE);
            }
        }

        final long last = data.getLastDay(colony.getID());
        if (last < 0)
        {
            // First day of the iron age: the first rations are taken tomorrow, so there is time to fill a granary.
            data.setLastDay(colony.getID(), day);
            return;
        }
        if (day <= last)
        {
            return;
        }
        final List<IItemHandler> stores = stores(colony, level);
        if (stores == null)
        {
            // A granary is not loaded: rations wait until a player is around.
            return;
        }
        handOut(colony, data, stores);
        data.setLastDay(colony.getID(), day);
    }

    private static void handOut(final IColony colony, final ColonyRationData data, final List<IItemHandler> stores)
    {
        int unfed = 0;
        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            if (takeRation(stores))
            {
                citizen.setSaturation(CitizenConstants.FULL_SATURATION);
                data.setHungryDays(colony.getID(), citizen.getId(), 0);
            }
            else
            {
                data.setHungryDays(colony.getID(), citizen.getId(), data.getHungryDays(colony.getID(), citizen.getId()) + 1);
                unfed++;
            }
            // Makes MineColonies count the happiness again.
            citizen.getCitizenHappinessHandler().addModifier(RationHappinessModifier.INSTANCE);
        }
        if (unfed > 0)
        {
            MessageUtils.format(Component.translatable(KEY + "unfed", unfed)).sendTo(colony).forAllPlayers();
        }
    }

    private static boolean takeRation(final List<IItemHandler> stores)
    {
        for (final IItemHandler store : stores)
        {
            for (int slot = 0; slot < store.getSlots(); slot++)
            {
                if (isRation(store.getStackInSlot(slot)) && !store.extractItem(slot, 1, false).isEmpty())
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Rations in the colony's granaries, or -1 when one of them is not loaded.
     */
    public static int stock(final IColony colony)
    {
        if (!(colony.getWorld() instanceof final ServerLevel level))
        {
            return -1;
        }
        final List<IItemHandler> stores = stores(colony, level);
        if (stores == null)
        {
            return -1;
        }
        int count = 0;
        for (final IItemHandler store : stores)
        {
            for (int slot = 0; slot < store.getSlots(); slot++)
            {
                final ItemStack stack = store.getStackInSlot(slot);
                if (isRation(stack))
                {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    /**
     * Whether the granaries could feed one more citizen, on top of the colony's, tomorrow. True when the era needs no food.
     */
    public static boolean canFeedNewcomer(final IColony colony)
    {
        if (!ColonyNeeds.rules(colony).food())
        {
            return true;
        }
        final int stock = stock(colony);
        return stock < 0 || stock > colony.getCitizenManager().getCurrentCitizenCount();
    }

    /**
     * Citizens that went without a ration yesterday.
     */
    public static int unfed(final IColony colony)
    {
        return colony.getWorld() instanceof final ServerLevel level ? ColonyRationData.get(level).hungryCitizens(colony.getID()) : 0;
    }

    /**
     * Food shelves and hangers in the rooms of the colony's built granaries, or null when one of them is not loaded.
     */
    private static List<IItemHandler> stores(final IColony colony, final ServerLevel level)
    {
        final List<IItemHandler> stores = new ArrayList<>();
        for (final IBuilding building : colony.getServerBuildingManager().getBuildings().values())
        {
            if (!building.getBuildingType().getRegistryName().equals(ModBuildings.GRANARY.get().getRegistryName()) || building.getBuildingLevel() < 1)
            {
                continue;
            }
            if (!WorldUtil.isEntityBlockLoaded(level, building.getPosition()))
            {
                return null;
            }
            final RoomScanner.Room room = RoomScanner.scan(level, building.getPosition(), BuildingRules.LevelRules.DEFAULT_MAX_VOLUME, false);
            for (final BlockPos pos : room.shell())
            {
                final IItemHandler store = store(level, pos);
                if (store != null)
                {
                    stores.add(store);
                }
            }
        }
        return stores;
    }

    private static IItemHandler store(final ServerLevel level, final BlockPos pos)
    {
        final ResourceLocation block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if (!block.getNamespace().equals(FIRMALIFE) || !CELLAR_STORE.matcher(block.getPath()).matches())
        {
            return null;
        }
        return inWorkingCellar(level.getBlockEntity(pos)) ? level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) : null;
    }

    /**
     * Whether a food shelf or hanger is in a cellar its climate station finds valid ({@code FoodShelfBlockEntity.isClimateValid}).
     */
    private static boolean inWorkingCellar(final BlockEntity entity)
    {
        if (!firmalifeLooked)
        {
            firmalifeLooked = true;
            try
            {
                isClimateValid = Class.forName("com.eerussianguy.firmalife.common.blockentities.FoodShelfBlockEntity").getMethod("isClimateValid");
            }
            catch (final ReflectiveOperationException e)
            {
                LegacyColonies.LOGGER.warn("Firmalife food shelves not found: no citizen can take a ration");
            }
        }
        if (isClimateValid == null || entity == null || !isClimateValid.getDeclaringClass().isInstance(entity))
        {
            return false;
        }
        try
        {
            return (boolean) isClimateValid.invoke(entity);
        }
        catch (final ReflectiveOperationException e)
        {
            return false;
        }
    }

    private static boolean isRation(final ItemStack stack)
    {
        return !stack.isEmpty() && stack.getFoodProperties(null) != null && !rotten(stack);
    }

    private static boolean rotten(final ItemStack stack)
    {
        if (!tfcLooked)
        {
            tfcLooked = true;
            try
            {
                isRotten = Class.forName("net.dries007.tfc.common.component.food.FoodCapability").getMethod("isRotten", ItemStack.class);
            }
            catch (final ReflectiveOperationException e)
            {
                LegacyColonies.LOGGER.warn("TFC food not found: rations do not check for rotten food");
            }
        }
        if (isRotten == null)
        {
            return false;
        }
        try
        {
            return (boolean) isRotten.invoke(null, stack);
        }
        catch (final ReflectiveOperationException e)
        {
            return false;
        }
    }
}
