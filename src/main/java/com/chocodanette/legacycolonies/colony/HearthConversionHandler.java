package com.chocodanette.legacycolonies.colony;

import com.chocodanette.legacycolonies.ModBlocks;
import com.chocodanette.legacycolonies.block.BlockColonyCampfire;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The colony hearth is not an item the player can make: right click a TFC firepit with the TFC field guide and the firepit
 * becomes the colony hearth, which founds the colony there. The field guide is used up.
 */
public final class HearthConversionHandler
{
    private static final ResourceLocation FIREPIT     = ResourceLocation.fromNamespaceAndPath("tfc", "firepit");
    private static final ResourceLocation GUIDE_BOOK  = ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book");
    private static final String           FIELD_GUIDE = "tfc:field_guide";

    private HearthConversionHandler()
    {
    }

    /**
     * The TFC field guide is a Patchouli guide book whose book component is {@code tfc:field_guide}. Read as text so that
     * the addon does not depend on Patchouli.
     */
    private static boolean isFieldGuide(final ItemStack stack)
    {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(GUIDE_BOOK) && stack.getComponentsPatch().toString().contains(FIELD_GUIDE);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event)
    {
        final Level level = event.getLevel();
        final BlockPos pos = event.getPos();
        final ItemStack stack = event.getItemStack();
        if (event.getHand() != InteractionHand.MAIN_HAND
              || !BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(FIREPIT)
              || !isFieldGuide(stack))
        {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        final Player player = event.getEntity();
        if (level.isClientSide || !player.mayUseItemAt(pos, event.getFace(), stack))
        {
            return;
        }
        if (IColonyManager.getInstance().getIColony(level, pos) != null)
        {
            player.displayClientMessage(Component.translatable("com.legacycolonies.hearth.in_colony"), true);
            return;
        }
        if (IColonyManager.getInstance().getIColonyByOwner(level, player) != null)
        {
            player.displayClientMessage(Component.translatable("com.legacycolonies.hearth.has_colony"), true);
            return;
        }

        // The firepit drops what it holds, then the hearth takes its place.
        level.removeBlock(pos, false);
        final BlockColonyCampfire hearth = ModBlocks.COLONY_CAMPFIRE.get();
        HutPlacementHandler.placeHut(level, pos, player, hearth, null, new ItemStack(hearth));
        if (!player.isCreative())
        {
            stack.shrink(1);
        }
    }
}
