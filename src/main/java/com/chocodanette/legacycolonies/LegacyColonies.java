package com.chocodanette.legacycolonies;

import com.minecolonies.api.colony.IColonyManager;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * MineColonies addon for the TerraFirmaCraft Legacy modpack.
 */
@Mod(LegacyColonies.MOD_ID)
public class LegacyColonies
{
    public static final String MOD_ID = "legacycolonies";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LegacyColonies(final IEventBus modBus)
    {
        modBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Legacy Colonies loaded, MineColonies colony manager: {}", IColonyManager.getInstance().getClass().getName());
    }
}
