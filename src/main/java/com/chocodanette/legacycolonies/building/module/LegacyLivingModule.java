package com.chocodanette.legacycolonies.building.module;

import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.modules.BedHandlingModule;
import com.minecolonies.core.colony.buildings.modules.LivingBuildingModule;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Living module of the Legacy huts: one citizen per bed. Only the beds registered in the building (placed by the builder,
 * or found when the building was confirmed) count, up to a number set by the building's level.
 */
public abstract class LegacyLivingModule extends LivingBuildingModule
{
    /**
     * Most beds the building may use at a level.
     */
    protected abstract int maxBeds(int level);

    @Override
    public int getModuleMax()
    {
        final int level = building.getBuildingLevel();
        return level > 0 ? Math.min(maxBeds(level), standingBeds()) : 0;
    }

    /**
     * Registered beds still in place. A bed in an unloaded chunk is taken as standing.
     */
    private int standingBeds()
    {
        final BedHandlingModule beds = building.getModule(BedHandlingModule.class);
        if (beds == null)
        {
            return 0;
        }
        final Level world = building.getColony().getWorld();
        int count = 0;
        for (final BlockPos pos : beds.getRegisteredBlocks())
        {
            if (world == null || !WorldUtil.isBlockLoaded(world, pos) || isBed(world.getBlockState(pos)))
            {
                count++;
            }
        }
        return count;
    }

    private static boolean isBed(final BlockState state)
    {
        return state.getBlock() instanceof BedBlock || state.is(BlockTags.BEDS);
    }
}
