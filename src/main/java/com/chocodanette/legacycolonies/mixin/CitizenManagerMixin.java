package com.chocodanette.legacycolonies.mixin;

import com.chocodanette.legacycolonies.needs.ColonyArrivals;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.colony.managers.CitizenManager;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

/**
 * The first citizens arrive at the edge of the colony and walk to the campfire, instead of appearing on it.
 */
@Mixin(value = CitizenManager.class, remap = false)
public abstract class CitizenManagerMixin
{
    @Shadow
    @Final
    private Colony colony;

    @ModifyArg(method = "onColonyTick", at = @At(value = "INVOKE",
      target = "Lcom/minecolonies/core/colony/managers/CitizenManager;spawnOrCreateCivilian(Lcom/minecolonies/api/colony/ICivilianData;Lnet/minecraft/world/level/Level;Ljava/util/List;Z)Lcom/minecolonies/api/colony/ICivilianData;"),
      index = 2)
    private List<BlockPos> legacycolonies$arriveFromTheEdge(final List<BlockPos> spawnPositions)
    {
        final BlockPos edge = ColonyArrivals.findEdgeSpawn(colony);
        return edge == null ? spawnPositions : List.of(edge);
    }
}
