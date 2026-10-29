package com.chocodanette.legacycolonies.mixin;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.interactionhandling.IInteractionResponseHandler;
import com.minecolonies.core.colony.CitizenData;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobBuilder;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * No complaints from citizens (no bed, hungry, no job, need barracks…). Item requests are kept, as the builder asks
 * for its materials through them, and so are the builder's own messages, which tell what blocks a construction.
 * Complaints saved before are dropped when the citizen loads. Quests (dialogues and deliveries) and notifications are
 * subclasses of the complaint interaction and stay.
 * <p>
 * No MineColonies hunger either: citizens stay full and never look for food. From the iron age, the colony is fed by
 * the daily rations of {@code ColonyRations} instead.
 */
@Mixin(value = CitizenData.class, remap = false)
public abstract class CitizenDataMixin
{
    @Shadow
    private double saturation;

    @Shadow
    @Final
    protected Map<Component, IInteractionResponseHandler> citizenChatOptions;

    @Inject(method = "triggerInteraction", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$noComplaints(final IInteractionResponseHandler handler, final CallbackInfo ci)
    {
        if (legacycolonies$isComplaint(handler) && !(((CitizenData) (Object) this).getJob() instanceof JobBuilder))
        {
            ci.cancel();
        }
    }

    @Inject(method = "decreaseSaturation", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$noHunger(final double extraSaturation, final CallbackInfo ci)
    {
        saturation = ICitizenData.MAX_SATURATION;
        ci.cancel();
    }

    @Inject(method = "deserializeNBT", at = @At("TAIL"))
    private void legacycolonies$forgetComplaints(final CallbackInfo ci)
    {
        saturation = ICitizenData.MAX_SATURATION;
        citizenChatOptions.values().removeIf(handler -> legacycolonies$isComplaint(handler));
    }

    /**
     * A plain complaint: exactly {@link StandardInteraction}, not one of its subclasses such as the quest dialogues.
     */
    private static boolean legacycolonies$isComplaint(final IInteractionResponseHandler handler)
    {
        return handler.getClass() == StandardInteraction.class;
    }
}
