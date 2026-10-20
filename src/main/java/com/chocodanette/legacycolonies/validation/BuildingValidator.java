package com.chocodanette.legacycolonies.validation;

import com.minecolonies.api.advancements.AdvancementTriggers;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.util.FireworkUtils;
import com.minecolonies.core.util.AdvancementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks a building the player designed around a hut ("confirm building" in the hut window) against the hut's rules, and raises the hut one level when it passes.
 */
public final class BuildingValidator
{
    private static final String KEY = "com.legacycolonies.confirm.";

    private BuildingValidator()
    {
    }

    public static void validate(final ServerPlayer player, final ServerLevel level, final BlockPos hutPos)
    {
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, hutPos);
        final IBuilding building = IColonyManager.getInstance().getBuilding(level, hutPos);
        if (colony == null || building == null)
        {
            fail(player, Component.translatable(KEY + "no_colony"));
            return;
        }
        if (!colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS))
        {
            fail(player, Component.translatable(KEY + "no_permission"));
            return;
        }

        final int target = building.getBuildingLevel() + 1;
        if (target > building.getMaxBuildingLevel())
        {
            fail(player, Component.translatable(KEY + "max_level"));
            return;
        }
        if (colony.getWorkManager().getWorkOrders().values().stream().anyMatch(order -> order.getLocation().equals(hutPos)))
        {
            fail(player, Component.translatable(KEY + "work_order"));
            return;
        }

        final BuildingRules rules = BuildingRulesManager.INSTANCE.get(building.getBuildingType().getRegistryName());
        final BuildingRules.LevelRules levelRules = rules == null ? null : rules.level(target).orElse(null);
        if (levelRules == null)
        {
            fail(player, Component.translatable(KEY + "no_rules", target));
            return;
        }

        final RoomScanner.Room room = RoomScanner.scan(level, hutPos, levelRules.maxVolume(), levelRules.closed());
        switch (room.problem())
        {
            case NOT_CLOSED -> fail(player, Component.translatable(KEY + "not_closed"));
            case TOO_BIG -> fail(player, Component.translatable(KEY + "too_big", levelRules.maxVolume()));
            case NOT_COVERED -> fail(player, Component.translatable(KEY + "not_covered"));
            case NONE -> { }
        }
        if (room.problem() != RoomScanner.Problem.NONE)
        {
            return;
        }

        final Set<BlockPos> blocks = new HashSet<>(room.shell());
        blocks.addAll(room.open());
        final List<Component> missing = new ArrayList<>();
        if (levelRules.door() && blocks.stream().noneMatch(pos -> isDoor(level.getBlockState(pos))))
        {
            missing.add(Component.translatable(KEY + "missing.door"));
        }
        if (levelRules.light() && blocks.stream().noneMatch(pos -> level.getBlockState(pos).getLightEmission() > 0))
        {
            missing.add(Component.translatable(KEY + "missing.light"));
        }
        for (final BuildingRules.Requirement requirement : levelRules.requires())
        {
            int found = 0;
            for (final BlockPos pos : blocks)
            {
                final BlockState state = level.getBlockState(pos);
                if (isMainPart(state) && requirement.matches(state))
                {
                    found++;
                }
            }
            if (found < requirement.count())
            {
                missing.add(Component.translatable(KEY + "missing.block", requirement.count() - found, requirement.displayName(), found, requirement.count()));
            }
        }

        if (!missing.isEmpty())
        {
            player.sendSystemMessage(Component.translatable(KEY + "failed", Component.translatable(building.getBuildingDisplayName()), target).withStyle(ChatFormatting.GOLD));
            missing.forEach(line -> player.sendSystemMessage(Component.literal(" - ").append(line)));
            return;
        }

        upgrade(colony, building, level, room, blocks, target);
        player.sendSystemMessage(Component.translatable(KEY + "success", Component.translatable(building.getBuildingDisplayName()), target).withStyle(ChatFormatting.GREEN));
    }

    /**
     * Same steps as the builder at the end of a work order, without a blueprint: the area of the building becomes the room.
     */
    private static void upgrade(final IColony colony, final IBuilding building, final ServerLevel level, final RoomScanner.Room room, final Set<BlockPos> blocks, final int target)
    {
        building.setBuildingLevel(target);
        building.onUpgradeComplete(null, target);
        building.setCorners(room.min(), room.max());
        for (final BlockPos pos : blocks)
        {
            if (pos.equals(building.getPosition()))
            {
                continue;
            }
            final BlockState state = level.getBlockState(pos);
            building.registerBlockPosition(state, pos, level);
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity)
            {
                building.addContainerPosition(pos);
            }
        }
        building.markDirty();

        FireworkUtils.spawnFireworksAtAABBCorners(new Tuple<>(room.min(), room.max()), level, target);
        AdvancementUtils.TriggerAdvancementPlayersForColony(colony,
          member -> AdvancementTriggers.COMPLETE_BUILD_REQUEST.get().trigger(member, building.getSchematicName(), target));
    }

    private static boolean isDoor(final BlockState state)
    {
        return state.getBlock() instanceof DoorBlock || state.is(BlockTags.DOORS);
    }

    /**
     * Beds and doors span two blocks: count each once.
     */
    private static boolean isMainPart(final BlockState state)
    {
        if (state.getBlock() instanceof BedBlock && state.hasProperty(BedBlock.PART))
        {
            return state.getValue(BedBlock.PART) == BedPart.HEAD;
        }
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))
        {
            return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
        }
        return true;
    }

    private static void fail(final ServerPlayer player, final Component message)
    {
        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.RED));
    }
}
