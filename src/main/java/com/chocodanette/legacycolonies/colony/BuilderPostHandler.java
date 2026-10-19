package com.chocodanette.legacycolonies.colony;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.ModBlocks;
import com.chocodanette.legacycolonies.building.BuildingColonyCampfire;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.eventbus.events.colony.ColonyCreatedModEvent;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Places the invisible builder's post above the colony campfire when a colony is founded.
 */
public final class BuilderPostHandler
{
    /**
     * Level of the post: the builder can build any level, Legacy stages gate the progression.
     */
    private static final int POST_LEVEL = 5;

    private BuilderPostHandler()
    {
    }

    public static void onColonyCreated(final ColonyCreatedModEvent event)
    {
        final IColony colony = event.getColony();
        final Level level = colony.getWorld();
        if (level == null || level.isClientSide || !(colony.getServerBuildingManager().getTownHall() instanceof final BuildingColonyCampfire campfire))
        {
            return;
        }

        // The campfire is a single block: it counts as built (level 1) as soon as the colony exists.
        if (campfire.getBuildingLevel() == 0)
        {
            campfire.setBuildingLevel(1);
            campfire.onUpgradeComplete(null, 1);
        }

        final BlockPos postPos = campfire.getPosition().above();
        if (!level.getBlockState(postPos).canBeReplaced())
        {
            LegacyColonies.LOGGER.warn("No room for the builder's post above the colony campfire at {}", campfire.getPosition());
            return;
        }

        level.setBlockAndUpdate(postPos, ModBlocks.BUILDER_POST.get().defaultBlockState());
        if (!(level.getBlockEntity(postPos) instanceof final AbstractTileEntityColonyBuilding hut))
        {
            LegacyColonies.LOGGER.error("The builder's post at {} has no hut block entity", postPos);
            return;
        }

        final IBuilding post = colony.getServerBuildingManager().addNewBuilding(hut, level);
        if (post != null)
        {
            post.setStructurePack(colony.getStructurePack());
            post.setBuildingLevel(POST_LEVEL);
            post.onUpgradeComplete(null, POST_LEVEL);
        }
    }

    /**
     * Removes the builder's post above a colony campfire that is being removed.
     */
    public static void removePost(final Level level, final BlockPos campfirePos)
    {
        final BlockPos postPos = campfirePos.above();
        if (!level.getBlockState(postPos).is(ModBlocks.BUILDER_POST.get()))
        {
            return;
        }

        final IBuilding post = IColonyManager.getInstance().getBuilding(level, postPos);
        if (post != null)
        {
            post.destroy();
        }
        level.removeBlock(postPos, false);
    }
}
