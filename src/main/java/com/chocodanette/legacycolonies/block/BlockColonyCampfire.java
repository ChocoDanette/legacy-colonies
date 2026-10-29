package com.chocodanette.legacycolonies.block;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.ModBuildings;
import com.chocodanette.legacycolonies.colony.BuilderPostHandler;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.core.blocks.huts.BlockHutTownHall;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Colony hearth (a TFC firepit look; the id stays {@code colony_campfire}): founds the colony and is its centre. Extends the town hall block, because MineColonies
 * recognises the colony centre with {@code instanceof BlockHutTownHall}.
 */
public class BlockColonyCampfire extends BlockHutTownHall
{
    public static final String NAME = "colony_campfire";

    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 0.125, 1);

    @NotNull
    @Override
    public String getHutName()
    {
        return NAME;
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return ResourceLocation.fromNamespaceAndPath(LegacyColonies.MOD_ID, NAME);
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.COLONY_CAMPFIRE.get();
    }

    @NotNull
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context)
    {
        return SHAPE;
    }

    @NotNull
    @Override
    protected VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos)
    {
        return Shapes.empty();
    }

    @Override
    public int getLightEmission(final BlockState state, final BlockGetter level, final BlockPos pos)
    {
        return 15;
    }

    @Override
    public void onRemove(final @NotNull BlockState state, final @NotNull Level level, final @NotNull BlockPos pos, final @NotNull BlockState newState, final boolean moved)
    {
        if (!level.isClientSide && !state.is(newState.getBlock()))
        {
            BuilderPostHandler.removePost(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
