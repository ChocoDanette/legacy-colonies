package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.minecolonies.api.quests.IDialogueObjectiveTemplate;
import com.minecolonies.api.quests.IQuestInstance;
import com.minecolonies.api.quests.IQuestManager;
import com.minecolonies.api.quests.IQuestObjectiveTemplate;
import com.minecolonies.api.quests.IQuestTemplate;
import com.minecolonies.core.quests.QuestManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A quest waiting on a dialogue opens it again with the colony. Legacy Colonies 0.9.1 test builds dropped quest
 * dialogues with the complaints, which left the quest offered by nobody until it timed out.
 */
@Mixin(value = QuestManager.class, remap = false)
public abstract class QuestManagerMixin
{
    @Inject(method = "onWorldLoad", at = @At("TAIL"))
    private void legacycolonies$reopenDialogues(final CallbackInfo ci)
    {
        final IQuestManager self = (IQuestManager) (Object) this;
        final List<IQuestInstance> quests = new ArrayList<>(self.getAvailableQuests());
        quests.addAll(self.getInProgressQuests());
        for (final IQuestInstance quest : quests)
        {
            try
            {
                final IQuestTemplate template = IQuestManager.GLOBAL_SERVER_QUESTS.get(quest.getId());
                if (template == null)
                {
                    continue;
                }
                final IQuestObjectiveTemplate objective = template.getObjective(quest.getObjectiveIndex());
                if (objective instanceof IDialogueObjectiveTemplate)
                {
                    objective.startObjective(quest);
                }
            }
            catch (final RuntimeException e)
            {
                LegacyColonies.LOGGER.warn("Could not open again the dialogue of quest {}", quest.getId(), e);
            }
        }
    }
}
