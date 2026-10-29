package com.chocodanette.legacycolonies.mixin;

import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenMournHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No mourning: when a citizen dies, the others keep working.
 */
@Mixin(value = CitizenMournHandler.class, remap = false)
public abstract class CitizenMournHandlerMixin
{
    @Inject(method = "addDeceasedCitizen", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$noDeceased(final String name, final CallbackInfo ci)
    {
        ci.cancel();
    }

    @Inject(method = "shouldMourn", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$neverShouldMourn(final CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(false);
    }

    @Inject(method = "isMourning", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$neverMourning(final CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(false);
    }
}
