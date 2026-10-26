package com.chocodanette.legacycolonies.mixin;

import com.minecolonies.core.colony.managers.ReproductionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No children: new citizens only come from outside, at the pace of the colony's needs (see {@code ColonyArrivals}).
 */
@Mixin(value = ReproductionManager.class, remap = false)
public abstract class ReproductionManagerMixin
{
    @Inject(method = "trySpawnChild", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$noChildren(final CallbackInfo ci)
    {
        ci.cancel();
    }
}
