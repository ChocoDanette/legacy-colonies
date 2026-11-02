package com.chocodanette.legacycolonies.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * A trait's label and description are translation keys of the modpack, or plain text (a key without translation is
 * shown as written).
 */
final class TraitText
{
    private TraitText()
    {
    }

    /**
     * "Agile, Strong": the labels, translated, separated by commas.
     */
    static MutableComponent list(final List<String> labels)
    {
        final MutableComponent text = Component.empty();
        for (int i = 0; i < labels.size(); i++)
        {
            if (i > 0)
            {
                text.append(", ");
            }
            text.append(Component.translatable(labels.get(i)));
        }
        return text;
    }
}
