package com.chocodanette.legacycolonies.needs;

import com.chocodanette.legacycolonies.ModBuildings;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /legacycolonies needs}: era, citizens, beds and houses of the colony the player stands in (or owns), and when the
 * next citizen arrives.
 */
public final class NeedsCommand
{
    private static final String KEY = "com.legacycolonies.needs.status.";

    private NeedsCommand()
    {
    }

    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("legacycolonies")
          .then(Commands.literal("needs").executes(NeedsCommand::show)));
    }

    private static int show(final CommandContext<CommandSourceStack> context)
    {
        final CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof final ServerPlayer player))
        {
            return 0;
        }
        IColony colony = IColonyManager.getInstance().getIColony(player.level(), player.blockPosition());
        if (colony == null)
        {
            colony = IColonyManager.getInstance().getIColonyByOwner(player.level(), player);
        }
        if (colony == null)
        {
            source.sendFailure(Component.translatable(KEY + "no_colony"));
            return 0;
        }

        final EraRules rules = ColonyNeeds.rules(colony);
        final int citizens = colony.getCitizenManager().getCurrentCitizenCount();
        final int allowed = ColonyNeeds.allowedCitizens(colony);
        source.sendSystemMessage(Component.translatable(KEY + "title", colony.getName(), ColonyNeeds.eraName(ColonyNeeds.getEra(colony))));
        source.sendSystemMessage(Component.translatable(KEY + "citizens", citizens, rules.maxCitizens()));
        source.sendSystemMessage(rules.beds()
          ? Component.translatable(KEY + "beds", ColonyNeeds.beds(colony))
          : Component.translatable(KEY + "no_beds_needed"));
        final EraRules.HutLimits houses = ColonyNeeds.limits(colony, ModBuildings.HOUSE.get());
        if (houses.maxCount() != EraRules.HutLimits.NO_LIMIT)
        {
            source.sendSystemMessage(Component.translatable(KEY + "houses", ColonyNeeds.count(colony, ModBuildings.HOUSE.get(), true, null), houses.maxCount()));
        }

        final long seconds = ColonyArrivals.secondsToNextArrival(colony);
        if (citizens >= rules.maxCitizens())
        {
            source.sendSystemMessage(Component.translatable(KEY + "full_era"));
        }
        else if (citizens >= allowed)
        {
            source.sendSystemMessage(Component.translatable(KEY + "need_beds"));
        }
        else if (seconds >= 0)
        {
            source.sendSystemMessage(Component.translatable(KEY + "next", Math.max(1, (seconds + 59) / 60)));
        }
        return 1;
    }
}
