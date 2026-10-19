package com.chocodanette.legacycolonies.block;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.chocodanette.legacycolonies.ModBuildings;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Invisible builder's post, placed above the colony campfire when the colony is founded. It cannot be seen,
 * targeted or broken by players; it disappears with the campfire.
 */
public class BlockBuilderPost extends AbstractBlockHut<BlockBuilderPost>
{
    public static final String NAME = "builder_post";

    public BlockBuilderPost()
    {
        super(Properties.of().noCollission().noOcclusion().noLootTable().strength(-1.0F, 3600000.0F));
    }

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
        return ModBuildings.BUILDER_POST.get();
    }

    @NotNull
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context)
    {
        return Shapes.empty();
    }

    @NotNull
    @Override
    protected RenderShape getRenderShape(final BlockState state)
    {
        return RenderShape.INVISIBLE;
    }
}
