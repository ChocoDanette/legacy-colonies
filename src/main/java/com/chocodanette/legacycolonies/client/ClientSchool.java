package com.chocodanette.legacycolonies.client;

import com.chocodanette.legacycolonies.network.SchoolPlanPayload;
import com.ldtteam.blockui.BOScreen;
import net.minecraft.client.Minecraft;

/**
 * Opens the school's heir window, or refreshes it when it is already open for the same hut.
 */
public final class ClientSchool
{
    private ClientSchool()
    {
    }

    public static void show(final SchoolPlanPayload plan)
    {
        if (Minecraft.getInstance().screen instanceof final BOScreen screen && screen.getWindow() instanceof final WindowSchool window
              && window.getPlan().hut().equals(plan.hut()))
        {
            window.update(plan);
            return;
        }
        new WindowSchool(plan).open();
    }
}
