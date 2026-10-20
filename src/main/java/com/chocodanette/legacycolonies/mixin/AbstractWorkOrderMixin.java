package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.material.MaterialSubstitution;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.minecolonies.api.colony.workorders.IWorkOrder;
import com.minecolonies.core.colony.workorders.AbstractWorkOrder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every plan a builder works from passes through {@link AbstractWorkOrder#setBlueprint}: swap its materials there.
 */
@Mixin(value = AbstractWorkOrder.class, remap = false)
public abstract class AbstractWorkOrderMixin
{
    @Inject(method = "setBlueprint", at = @At("HEAD"))
    private void legacycolonies$substituteMaterials(final Blueprint blueprint, final Level world, final CallbackInfo ci)
    {
        MaterialSubstitution.apply(blueprint, world, ((IWorkOrder) (Object) this).getLocation());
    }
}
