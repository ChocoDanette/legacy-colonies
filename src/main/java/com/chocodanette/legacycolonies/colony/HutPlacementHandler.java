package com.chocodanette.legacycolonies.colony;

import com.chocodanette.legacycolonies.block.BlockBuilderPost;
import com.chocodanette.legacycolonies.block.BlockColonyCampfire;
import com.chocodanette.legacycolonies.needs.ColonyNeeds;
import com.ldtteam.structurize.storage.ServerFutureProcessor;
import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Right click a block with a hut to place the hut there, unbuilt (level 0). The player then builds around it and confirms
 * the building from the hut's window, or asks the builder; breaking the hut gives it back.
 * <p>
 * This replaces MineColonies' "use the build tool" window. Right clicking the air with a hut opens the build tool
 * instead (see {@code ClientHutHandler}).
 */
public final class HutPlacementHandler
{
    private HutPlacementHandler()
    {
    }

    public static boolean isPlaceableHut(final ItemStack stack)
    {
        return stack.getItem() instanceof final BlockItem item
                 && item.getBlock() instanceof AbstractBlockHut<?>
                 && !(item.getBlock() instanceof BlockBuilderPost);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event)
    {
        final ItemStack stack = event.getItemStack();
        if (!isPlaceableHut(stack))
        {
            return;
        }
        final AbstractBlockHut<?> hutBlock = (AbstractBlockHut<?>) ((BlockItem) stack.getItem()).getBlock();

        final Level level = event.getLevel();
        final Player player = event.getEntity();
        // Right-clicking a hut opens its window: leave that to MineColonies.
        if (level.getBlockState(event.getPos()).getBlock() instanceof AbstractBlockHut<?>)
        {
            return;
        }

        final BlockPos pos = level.getBlockState(event.getPos()).canBeReplaced() ? event.getPos() : event.getPos().relative(event.getFace());
        if (!level.getBlockState(pos).canBeReplaced() || !player.mayUseItemAt(pos, event.getFace(), stack))
        {
            return;
        }

        final IColony colony = IColonyManager.getInstance().getIColony(level, pos);
        final boolean campfire = hutBlock instanceof BlockColonyCampfire;
        // Inside a colony, MineColonies refuses a second colony centre itself.
        if (campfire && colony != null)
        {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide)
        {
            return;
        }

        if (!campfire && colony == null)
        {
            player.displayClientMessage(Component.translatable("com.minecolonies.coremod.placement.outofcolony"), true);
            return;
        }
        if (colony != null && !colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS))
        {
            player.displayClientMessage(Component.translatable("com.minecolonies.coremod.placement.noperm"), true);
            return;
        }
        final Component eraRefusal = colony == null ? null : ColonyNeeds.refusePlacement(colony, hutBlock.getBuildingEntry());
        if (eraRefusal != null)
        {
            player.displayClientMessage(eraRefusal, true);
            return;
        }

        final BlockState state = hutBlock.defaultBlockState().setValue(AbstractBlockHut.FACING, player.getDirection());
        level.setBlockAndUpdate(pos, state);
        final StructurePackMeta pack = packFor(colony);
        final String legacyPath = pack == null || !LegacyBlueprints.isLegacyHut(hutBlock.getBuildingEntry()) ? null
                                    : LegacyBlueprints.find(pack, hutBlock.getBuildingEntry(), 1);
        if (level.getBlockEntity(pos) instanceof final TileEntityColonyBuilding hut && legacyPath != null)
        {
            // Set before the building is created below, so that it starts with its plan.
            hut.setStructurePack(pack);
            hut.setBlueprintPath(legacyPath);
        }
        else if (level.getBlockEntity(pos) instanceof final TileEntityColonyBuilding hut && pack != null)
        {
            // Same lookup as MineColonies' direct placement: the level 1 blueprint of the pack whose anchor is this block.
            hut.setStructurePack(pack);
            ServerFutureProcessor.queueBlueprint(new ServerFutureProcessor.BlueprintProcessingData(
              StructurePacks.findBlueprintFuture(pack.getName(),
                blueprint -> blueprint.getBlockState(blueprint.getPrimaryBlockOffset()).getBlock() == hutBlock,
                level.registryAccess()),
              level,
              blueprint -> {
                  if (blueprint != null)
                  {
                      // relativize, not a "/" replace: on Windows the paths use backslashes.
                      final String folder = pack.getPath().relativize(blueprint.getFilePath()).toString().replace('\\', '/');
                      final String name = blueprint.getFileName();
                      hut.setBlueprintPath((folder.isEmpty() ? "" : folder + "/") + name.substring(0, name.length() - 1) + "1.blueprint");
                  }
              }));
        }
        state.getBlock().setPlacedBy(level, pos, state, player, stack);
        if (!player.isCreative())
        {
            stack.shrink(1);
        }
    }

    @Nullable
    private static StructurePackMeta packFor(@Nullable final IColony colony)
    {
        if (LegacyBlueprints.pack() != null)
        {
            return LegacyBlueprints.pack();
        }
        if (colony != null && StructurePacks.hasPack(colony.getStructurePack()))
        {
            return StructurePacks.getStructurePack(colony.getStructurePack());
        }
        return StructurePacks.selectedPack;
    }
}
