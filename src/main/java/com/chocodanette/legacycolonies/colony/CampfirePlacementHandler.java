package com.chocodanette.legacycolonies.colony;

import com.chocodanette.legacycolonies.block.BlockColonyCampfire;
import com.ldtteam.structurize.storage.ServerFutureProcessor;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Lets players place the colony campfire by hand, outside any colony.
 * <p>
 * MineColonies opens its "use the build tool" window for every hut, and its direct placement only accepts its own
 * town hall block outside a colony. This places the campfire like MineColonies' direct placement does for the town hall.
 */
public final class CampfirePlacementHandler
{
    private CampfirePlacementHandler()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event)
    {
        final ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof final BlockItem item) || !(item.getBlock() instanceof final BlockColonyCampfire campfire))
        {
            return;
        }

        final Level level = event.getLevel();
        final Player player = event.getEntity();
        // Right-clicking a hut opens its window: leave that to MineColonies.
        if (level.getBlockState(event.getPos()).getBlock() instanceof AbstractBlockHut<?>)
        {
            return;
        }

        final BlockPos pos = level.getBlockState(event.getPos()).canBeReplaced() ? event.getPos() : event.getPos().relative(event.getFace());
        // Inside a colony, MineColonies refuses a second colony centre itself.
        if (IColonyManager.getInstance().getIColony(level, pos) != null || !level.getBlockState(pos).canBeReplaced() || !player.mayUseItemAt(pos, event.getFace(), stack))
        {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide)
        {
            return;
        }

        final BlockState state = campfire.defaultBlockState().setValue(AbstractBlockHut.FACING, player.getDirection());
        level.setBlockAndUpdate(pos, state);
        if (level.getBlockEntity(pos) instanceof final TileEntityColonyBuilding hut && StructurePacks.selectedPack != null)
        {
            // Same lookup as MineColonies' direct placement: the level 1 blueprint of the selected pack whose anchor is this block.
            hut.setStructurePack(StructurePacks.selectedPack);
            final String packPath = StructurePacks.selectedPack.getPath().toString() + "/";
            ServerFutureProcessor.queueBlueprint(new ServerFutureProcessor.BlueprintProcessingData(
              StructurePacks.findBlueprintFuture(StructurePacks.selectedPack.getName(),
                blueprint -> blueprint.getBlockState(blueprint.getPrimaryBlockOffset()).getBlock() == campfire,
                level.registryAccess()),
              level,
              blueprint -> {
                  if (blueprint != null)
                  {
                      final String folder = blueprint.getFilePath().toString().replace(packPath, "");
                      final String name = blueprint.getFileName();
                      hut.setBlueprintPath(folder + "/" + name.substring(0, name.length() - 1) + "1.blueprint");
                  }
              }));
        }
        state.getBlock().setPlacedBy(level, pos, state, player, stack);
        if (!player.isCreative())
        {
            stack.shrink(1);
        }
    }
}
