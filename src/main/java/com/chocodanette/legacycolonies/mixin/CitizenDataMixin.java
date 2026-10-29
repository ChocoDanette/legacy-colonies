package com.chocodanette.legacycolonies.mixin;

import com.minecolonies.api.colony.interactionhandling.IInteractionResponseHandler;
import com.minecolonies.core.colony.CitizenData;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No complaints from citizens (no bed, hungry, no job…). Item requests are kept, as the builder asks for its
 * materials through them, and so are the builder's own messages, which tell what blocks a construction.
 */
@Mixin(value = CitizenData.class, remap = false)
public abstract class CitizenDataMixin
{
    @Inject(method = "triggerInteraction", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$noComplaints(final IInteractionResponseHandler handler, final CallbackInfo ci)
    {
        if (handler instanceof StandardInteraction && !(((CitizenData) (Object) this).getJob() instanceof JobBuilder))
        {
            ci.cancel();
        }
    }
}
