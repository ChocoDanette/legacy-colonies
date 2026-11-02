package com.chocodanette.legacycolonies.client;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.network.CharacterSheetPayload;
import com.chocodanette.legacycolonies.network.CharacterSheetPayload.StatRow;
import com.chocodanette.legacycolonies.network.CharacterSheetPayload.TraitRow;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The character window: name, generation, spouse and colony; traits on the left; stats and the heir's plan on the right.
 */
public class WindowCharacter extends AbstractWindowSkeleton
{
    private static final String KEY = "com.legacycolonies.character.";

    /**
     * Closer than this to the base, a stat is ordinary.
     */
    private static final double EPSILON = 0.001;

    private CharacterSheetPayload sheet;
    private final ScrollingList traitList;
    private final ScrollingList statList;

    public WindowCharacter(final CharacterSheetPayload sheet)
    {
        super(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "gui/windowcharacter.xml"));
        this.sheet = sheet;
        traitList = findPaneOfTypeByID("traits", ScrollingList.class);
        statList = findPaneOfTypeByID("stats", ScrollingList.class);
        registerButton("cancel", this::close);
        traitList.setDataProvider(() -> this.sheet.traits().size(), (index, row) -> updateTrait(row, this.sheet.traits().get(index)));
        statList.setDataProvider(() -> this.sheet.stats().size(), (index, row) -> updateStat(row, this.sheet.stats().get(index)));
        refresh();
    }

    public void update(final CharacterSheetPayload newSheet)
    {
        sheet = newSheet;
        refresh();
    }

    private void refresh()
    {
        findPaneOfTypeByID("title", Text.class).setText(sheet.fullName().isEmpty() ? Component.translatable(KEY + "unknown") : Component.literal(sheet.fullName()));
        findPaneOfTypeByID("identity", Text.class).setText(identity());
        final Text noTraits = findPaneOfTypeByID("notraits", Text.class);
        noTraits.setText(Component.translatable(KEY + "traits.none"));
        noTraits.setVisible(sheet.traits().isEmpty());
        findPaneOfTypeByID("heir", Text.class).setText(heir());
        traitList.refreshElementPanes();
        statList.refreshElementPanes();
    }

    /**
     * "Generation 2 · Married to Bertrand · Colony: Rochebrune".
     */
    private MutableComponent identity()
    {
        final MutableComponent text = Component.translatable(KEY + "generation", sheet.generation());
        text.append(" · ").append(sheet.spouse().isEmpty() ? Component.translatable(KEY + "unmarried") : Component.translatable(KEY + "married", sheet.spouse()));
        if (!sheet.colony().isEmpty())
        {
            text.append(" · ").append(Component.translatable(KEY + "colony", sheet.colony()));
        }
        return text;
    }

    private MutableComponent heir()
    {
        if (sheet.spouse().isEmpty())
        {
            return Component.translatable(KEY + "heir.unmarried");
        }
        if (sheet.added().isEmpty() && sheet.dropped().isEmpty())
        {
            return Component.translatable(KEY + "heir.none");
        }
        return Component.translatable(KEY + "heir.plan",
          sheet.added().isEmpty() ? Component.translatable(KEY + "heir.nothing") : Component.literal(String.join(", ", sheet.added())),
          sheet.dropped().isEmpty() ? Component.translatable(KEY + "heir.nothing") : Component.literal(String.join(", ", sheet.dropped())));
    }

    private void updateTrait(final Pane row, final TraitRow trait)
    {
        row.findPaneOfTypeByID("name", Text.class).setText(Component.literal(trait.label()));
        row.findPaneOfTypeByID("desc", Text.class).setText(Component.literal(trait.description()));
    }

    /**
     * "Max health  24 (+4)", the difference in green when it helps the character and in red when it hinders them.
     */
    private void updateStat(final Pane row, final StatRow stat)
    {
        row.findPaneOfTypeByID("name", Text.class).setText(Component.translatable(stat.label()));
        final MutableComponent value = Component.literal(stat.multiplier() ? "×" + number(stat.value()) : number(stat.value()));
        if (!stat.unit().isEmpty())
        {
            value.append(" ").append(Component.translatable(stat.unit()));
        }
        final double delta = stat.value() - stat.base();
        if (stat.showDelta() && Math.abs(delta) > EPSILON)
        {
            final boolean good = delta > 0 != stat.lowerIsBetter();
            final String sign = delta > 0 ? "+" : "-";
            final String amount = stat.multiplier() ? sign + Math.round(Math.abs(delta / stat.base()) * 100) + " %" : sign + number(Math.abs(delta));
            value.append(" ").append(Component.literal("(" + amount + ")").withStyle(good ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
        }
        row.findPaneOfTypeByID("value", Text.class).setText(value);
    }

    /**
     * At most two decimals, without trailing zeros, with a comma in French.
     */
    private static String number(final double value)
    {
        final String text = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("fr") ? text.replace('.', ',') : text;
    }
}
