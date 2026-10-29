package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.pile.MaterialPiles;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import com.minecolonies.core.entity.ai.workers.util.BuildingStructureHandler;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * The builder places the pile's materials only with items it took from the pile (see {@link MaterialPiles#mayPlace}).
 */
@Mixin(value = BuildingStructureHandler.class, remap = false)
public abstract class BuildingStructureHandlerMixin
{
    @Inject(method = "hasRequiredItems", at = @At("HEAD"), cancellable = true)
    private void legacycolonies$pileItemsOnly(final List<ItemStack> requiredItems, final CallbackInfoReturnable<Boolean> cir)
    {
        if (((BuildingStructureHandlerAccessor) this).legacycolonies$getStructureAI() instanceof EntityAIStructureBuilder builder
              && !MaterialPiles.mayPlace(builder, requiredItems))
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "consume", at = @At("HEAD"))
    private void legacycolonies$usePileItems(final List<ItemStack> requiredItems, final CallbackInfo ci)
    {
        if (((BuildingStructureHandlerAccessor) this).legacycolonies$getStructureAI() instanceof EntityAIStructureBuilder builder)
        {
            MaterialPiles.onConsume(builder, requiredItems);
        }
    }
}
