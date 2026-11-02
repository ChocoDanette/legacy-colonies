package com.chocodanette.legacycolonies.client;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.network.SchoolChangePayload;
import com.chocodanette.legacycolonies.network.SchoolPlanPayload;
import com.chocodanette.legacycolonies.network.SchoolPlanPayload.TraitRow;
import com.chocodanette.legacycolonies.school.HeirSchool;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The school's heir window: the character's traits, each with a "drop" button, and the other traits, each with an
 * "add" button. The server decides which buttons work; each click sends the change and the server sends the window back.
 */
public class WindowSchool extends AbstractWindowSkeleton
{
    private static final String KEY = "com.legacycolonies.school.";

    private SchoolPlanPayload plan;
    private final ScrollingList currentList;
    private final ScrollingList othersList;

    public WindowSchool(final SchoolPlanPayload plan)
    {
        super(ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, "gui/windowschool.xml"));
        this.plan = plan;
        currentList = findPaneOfTypeByID("current", ScrollingList.class);
        othersList = findPaneOfTypeByID("others", ScrollingList.class);
        registerButton("cancel", this::close);
        registerButton("drop", button -> change(button, currentList, true));
        registerButton("add", button -> change(button, othersList, false));
        currentList.setDataProvider(() -> this.plan.current().size(), (index, row) -> updateRow(row, this.plan.current().get(index), true));
        othersList.setDataProvider(() -> this.plan.others().size(), (index, row) -> updateRow(row, this.plan.others().get(index), false));
        refresh();
    }

    public SchoolPlanPayload getPlan()
    {
        return plan;
    }

    public void update(final SchoolPlanPayload newPlan)
    {
        plan = newPlan;
        refresh();
    }

    private void refresh()
    {
        final String name = plan.firstName().isEmpty() ? "?" : plan.firstName();
        findPaneOfTypeByID("title", Text.class).setText(Component.translatable(KEY + "title", name));
        findPaneOfTypeByID("summary", Text.class).setText(Component.translatable(KEY + "summary", plan.level(), plan.changes(), plan.level(), HeirSchool.MAX_TRAITS)
          .append(plan.married() ? Component.empty() : Component.literal(" ").append(Component.translatable(KEY + "unmarried"))));
        findPaneOfTypeByID("plan", Text.class).setText(planText());
        currentList.refreshElementPanes();
        othersList.refreshElementPanes();
    }

    /**
     * "Heir: added and kept traits, dropped ones, and the rest left to chance".
     */
    private MutableComponent planText()
    {
        final List<String> sure = new ArrayList<>();
        final List<String> dropped = new ArrayList<>();
        plan.others().stream().filter(TraitRow::chosen).forEach(row -> sure.add(row.label()));
        plan.current().stream().filter(TraitRow::chosen).forEach(row -> dropped.add(row.label()));
        if (sure.isEmpty() && dropped.isEmpty())
        {
            return Component.translatable(KEY + "plan.none");
        }
        return Component.translatable(KEY + "plan",
          sure.isEmpty() ? Component.translatable(KEY + "plan.nothing") : TraitText.list(sure),
          dropped.isEmpty() ? Component.translatable(KEY + "plan.nothing") : TraitText.list(dropped));
    }

    private void updateRow(final Pane row, final TraitRow trait, final boolean current)
    {
        final MutableComponent name = Component.translatable(trait.label());
        if (trait.chosen())
        {
            name.append(" ").append(Component.translatable(KEY + (current ? "dropped" : "added")).withStyle(current ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GREEN));
        }
        row.findPaneOfTypeByID("name", Text.class).setText(name);
        row.findPaneOfTypeByID("desc", Text.class).setText(Component.translatable(trait.description()));
        final Button button = row.findPaneOfTypeByID(current ? "drop" : "add", Button.class);
        button.setText(Component.translatable(KEY + (trait.chosen() ? (current ? "keep" : "cancel") : (current ? "drop" : "add"))));
        button.setEnabled(trait.available());
    }

    private void change(final Button button, final ScrollingList list, final boolean current)
    {
        final int index = list.getListElementIndexByPane(button);
        final List<TraitRow> rows = current ? plan.current() : plan.others();
        if (index < 0 || index >= rows.size() || !rows.get(index).available())
        {
            return;
        }
        final TraitRow row = rows.get(index);
        final HeirSchool.Change change = row.chosen() ? HeirSchool.Change.CANCEL : (current ? HeirSchool.Change.DROP : HeirSchool.Change.ADD);
        PacketDistributor.sendToServer(new SchoolChangePayload(plan.hut(), change, row.id()));
    }
}
