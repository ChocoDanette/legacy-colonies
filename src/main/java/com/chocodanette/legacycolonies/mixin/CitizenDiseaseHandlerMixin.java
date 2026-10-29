package com.chocodanette.legacycolonies.mixin;

import com.minecolonies.core.datalistener.model.Disease;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenDiseaseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No diseases and no trip to the hospital: citizens never get sick, a citizen already sick is cured, and a hurt
 * citizen keeps working instead of looking for a doctor.
 */
@Mixin(value = CitizenDiseaseHandler.class, remap = false)
public abstract class CitizenDiseaseHandlerMixin
{
    @Shadow
    private Disease disease;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$noDisease(final int tickRate, final CallbackInfo ci)
    {
        disease = null;
        ci.cancel();
    }

    @Inject(method = "setDisease", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$noNewDisease(final Disease disease, final CallbackInfoReturnable<Boolean> cir)
    {
        if (disease != null)
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isHurt", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$neverSeeksDoctor(final CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(false);
    }
}
