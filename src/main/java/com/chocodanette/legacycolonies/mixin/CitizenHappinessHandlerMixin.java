package com.chocodanette.legacycolonies.mixin;

import com.minecolonies.api.entity.citizen.happiness.IHappinessModifier;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenHappinessHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;

import static com.minecolonies.api.util.constant.HappinessConstants.*;

/**
 * Citizens' happiness only counts what the Legacy colony asks for: a bed, sleep, the Legacy rations, and the events
 * MineColonies keeps (damage, deaths, quests). The MineColonies needs that call for its own huts or mechanics
 * (barracks, tavern, mystical site, school, food variety, jobs, hospital) are removed, with their complaints.
 */
@Mixin(value = CitizenHappinessHandler.class, remap = false)
public abstract class CitizenHappinessHandlerMixin
{
    private static final Set<String> LEGACYCOLONIES$REMOVED =
      Set.of(UNEMPLOYMENT, HEALTH, IDLEATJOB, SCHOOL, MYSTICAL_SITE, SECURITY, SOCIAL, FOOD, HADGREATFOOD);

    @Shadow
    public Map<String, IHappinessModifier> happinessFactors;

    @Shadow
    private double cachedHappiness;

    @Inject(method = "addModifier", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$noRemovedNeed(final IHappinessModifier modifier, final CallbackInfo ci)
    {
        if (LEGACYCOLONIES$REMOVED.contains(modifier.getId()))
        {
            ci.cancel();
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void legacycolonies$removeSavedNeeds(final CallbackInfo ci)
    {
        happinessFactors.keySet().removeAll(LEGACYCOLONIES$REMOVED);
        cachedHappiness = -1;
    }

    /**
     * With fewer needs, every remaining one can be neutral (factor 1), which MineColonies leaves out of the average:
     * it then divides by zero. A citizen with nothing to complain about is fully happy.
     */
    @Inject(method = "getHappiness", at = @At("RETURN"), cancellable = true)
    private void legacycolonies$happyWithoutNeeds(final CallbackInfoReturnable<Double> cir)
    {
        if (Double.isNaN(cir.getReturnValueD()))
        {
            cachedHappiness = 10.0;
            cir.setReturnValue(10.0);
        }
    }
}
