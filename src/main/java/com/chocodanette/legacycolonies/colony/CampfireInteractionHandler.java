package com.chocodanette.legacycolonies.colony;

import com.chocodanette.legacycolonies.ModBlocks;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingMiner;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The builder's post above the colony campfire cannot be reached, so the campfire gives access to it:
 * sneak and right click with an empty hand to open the builder's stock, right click with a TFC dirt block to make it the
 * block the builder fills the ground with.
 */
public final class CampfireInteractionHandler
{
    private static final TagKey<Item>     TFC_DIRT     = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("tfc", "dirt"));
    private static final ResourceLocation DEFAULT_FILL = ResourceLocation.fromNamespaceAndPath("tfc", "dirt/mollisol");

    private CampfireInteractionHandler()
    {
    }

    @SubscribeEvent
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getLevel().getBlockState(event.getPos()).is(ModBlocks.COLONY_CAMPFIRE.get()))
        {
            return;
        }

        final ItemStack stack = event.getItemStack();
        final boolean openStock = stack.isEmpty() && event.getEntity().isShiftKeyDown();
        final boolean chooseFill = stack.is(TFC_DIRT) && !stack.is(Items.DIRT) && stack.getItem() instanceof BlockItem;
        if (!openStock && !chooseFill)
        {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getLevel() instanceof final ServerLevel level) || !(event.getEntity() instanceof final ServerPlayer player))
        {
            return;
        }

        final BlockPos postPos = event.getPos().above();
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, event.getPos());
        final IBuilding post = IColonyManager.getInstance().getBuilding(level, postPos);
        if (colony == null || post == null || !colony.getPermissions().hasPermission(player, Action.ACCESS_HUTS))
        {
            return;
        }

        if (openStock)
        {
            if (level.getBlockEntity(postPos) instanceof final TileEntityColonyBuilding hut)
            {
                player.openMenu(hut, buf -> buf.writeVarInt(colony.getID()).writeBlockPos(postPos));
            }
            return;
        }

        setFill(post, (BlockItem) stack.getItem());
        player.displayClientMessage(Component.translatable("com.legacycolonies.fill.chosen", stack.getHoverName()), true);
    }

    /**
     * Fill the ground of a new colony with the dirt it stands on.
     */
    public static void setDefaultFill(final IBuilding post, final Level level, final BlockPos campfirePos)
    {
        final ResourceLocation ground = BuiltInRegistries.BLOCK.getKey(level.getBlockState(campfirePos.below()).getBlock());
        final String soil = ground.getPath().substring(ground.getPath().lastIndexOf('/') + 1);
        final ResourceLocation dirt = ResourceLocation.fromNamespaceAndPath("tfc", "dirt/" + soil);
        final Item item = BuiltInRegistries.ITEM.get(ground.getNamespace().equals("tfc") && BuiltInRegistries.ITEM.containsKey(dirt) ? dirt : DEFAULT_FILL);
        if (item instanceof final BlockItem blockItem)
        {
            setFill(post, blockItem);
        }
    }

    private static void setFill(final IBuilding post, final BlockItem fill)
    {
        post.getSetting(BuildingMiner.FILL_BLOCK).setValue(fill);
        post.markDirty();
    }
}
