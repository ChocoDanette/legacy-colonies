package com.chocodanette.legacycolonies.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The builder's register: right click a hut with it to validate a building of the player's own design.
 * The check itself is in {@link com.chocodanette.legacycolonies.validation.RegisterHandler}.
 */
public class ItemRegister extends Item
{
    public static final String NAME = "register";

    public ItemRegister(final Properties properties)
    {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull final ItemStack stack, @NotNull final TooltipContext context, @NotNull final List<Component> tooltip, @NotNull final TooltipFlag flag)
    {
        tooltip.add(Component.translatable("item.legacycolonies.register.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
