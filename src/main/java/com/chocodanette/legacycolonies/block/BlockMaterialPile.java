package com.chocodanette.legacycolonies.block;

import com.chocodanette.legacycolonies.pile.MaterialPileBlockEntity;
import com.chocodanette.legacycolonies.pile.MaterialPiles;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The material pile of a work order (see {@link MaterialPiles}): it grows as the player brings the materials. Placed
 * and removed by the builder only. Right click with an item puts it in, crouching puts in every needed item of the
 * inventory, empty hand lists what is missing.
 */
public class BlockMaterialPile extends Block implements EntityBlock
{
    public static final String NAME = "material_pile";

    public static final MapCodec<BlockMaterialPile> CODEC = simpleCodec(properties -> new BlockMaterialPile());

    public static final int             MAX_FILL = 3;
    public static final IntegerProperty FILL     = IntegerProperty.create("fill", 0, MAX_FILL);

    private static final VoxelShape[] SHAPES = {
      Shapes.box(0, 0, 0, 1, 0.25, 1),
      Shapes.box(0, 0, 0, 1, 0.5, 1),
      Shapes.box(0, 0, 0, 1, 0.75, 1),
      Shapes.block()
    };

    public BlockMaterialPile()
    {
        super(BlockBehaviour.Properties.of()
          .strength(-1.0F, 3_600_000.0F)
          .noLootTable()
          .sound(SoundType.WOOD)
          .noOcclusion()
          .pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    @NotNull
    @Override
    protected MapCodec<? extends Block> codec()
    {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FILL);
    }

    @NotNull
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context)
    {
        return SHAPES[state.getValue(FILL)];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull final BlockPos pos, @NotNull final BlockState state)
    {
        return new MaterialPileBlockEntity(pos, state);
    }

    @NotNull
    @Override
    protected ItemInteractionResult useItemOn(
      @NotNull final ItemStack stack,
      @NotNull final BlockState state,
      @NotNull final Level level,
      @NotNull final BlockPos pos,
      @NotNull final Player player,
      @NotNull final InteractionHand hand,
      @NotNull final BlockHitResult hit)
    {
        if (!(level.getBlockEntity(pos) instanceof MaterialPileBlockEntity pile))
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player instanceof ServerPlayer serverPlayer)
        {
            MaterialPiles.deposit(serverPlayer, pile, player.isShiftKeyDown() ? null : stack);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @NotNull
    @Override
    protected InteractionResult useWithoutItem(
      @NotNull final BlockState state,
      @NotNull final Level level,
      @NotNull final BlockPos pos,
      @NotNull final Player player,
      @NotNull final BlockHitResult hit)
    {
        if (!(level.getBlockEntity(pos) instanceof MaterialPileBlockEntity pile))
        {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer)
        {
            if (player.isShiftKeyDown())
            {
                MaterialPiles.deposit(serverPlayer, pile, null);
            }
            else
            {
                MaterialPiles.showMissing(serverPlayer, pile);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(@NotNull final BlockState state, @NotNull final Level level, @NotNull final BlockPos pos, @NotNull final BlockState oldState, final boolean movedByPiston)
    {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this))
        {
            level.scheduleTick(pos, this, MaterialPiles.CHECK_INTERVAL);
        }
    }

    /**
     * Every few seconds: a pile whose work order is gone drops what it holds and disappears.
     */
    @Override
    protected void tick(@NotNull final BlockState state, @NotNull final ServerLevel level, @NotNull final BlockPos pos, @NotNull final RandomSource random)
    {
        if (level.getBlockEntity(pos) instanceof MaterialPileBlockEntity pile && !MaterialPiles.checkStillNeeded(level, pile))
        {
            return;
        }
        level.scheduleTick(pos, this, MaterialPiles.CHECK_INTERVAL);
    }
}
