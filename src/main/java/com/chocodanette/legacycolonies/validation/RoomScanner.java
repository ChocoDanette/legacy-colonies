package com.chocodanette.legacycolonies.validation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Finds the room around a hut: the open blocks reachable from the hut, and the blocks that enclose them.
 */
public final class RoomScanner
{
    private RoomScanner()
    {
    }

    /**
     * @param open    blocks a player could walk through (air, torches...), inside the room.
     * @param shell   blocks touching the open ones: floor, walls, roof, furniture, doors. Includes the hut.
     * @param closed  false when the open blocks reach the sky or exceed the volume limit.
     */
    public record Room(Set<BlockPos> open, Set<BlockPos> shell, boolean closed)
    {
        public BlockPos min()
        {
            int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE, z = Integer.MAX_VALUE;
            for (final BlockPos pos : shell)
            {
                x = Math.min(x, pos.getX());
                y = Math.min(y, pos.getY());
                z = Math.min(z, pos.getZ());
            }
            return new BlockPos(x, y, z);
        }

        public BlockPos max()
        {
            int x = Integer.MIN_VALUE, y = Integer.MIN_VALUE, z = Integer.MIN_VALUE;
            for (final BlockPos pos : shell)
            {
                x = Math.max(x, pos.getX());
                y = Math.max(y, pos.getY());
                z = Math.max(z, pos.getZ());
            }
            return new BlockPos(x, y, z);
        }
    }

    public static Room scan(final Level level, final BlockPos hut, final int maxVolume)
    {
        final Set<BlockPos> open = new HashSet<>();
        final Set<BlockPos> shell = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        shell.add(hut);
        queue.add(hut);

        while (!queue.isEmpty())
        {
            final BlockPos current = queue.poll();
            for (final Direction direction : Direction.values())
            {
                final BlockPos next = current.relative(direction);
                if (open.contains(next) || shell.contains(next))
                {
                    continue;
                }
                if (!level.isLoaded(next))
                {
                    return new Room(open, shell, false);
                }
                if (!level.getBlockState(next).getCollisionShape(level, next).isEmpty())
                {
                    shell.add(next);
                    continue;
                }
                if (level.canSeeSky(next) || open.size() >= maxVolume)
                {
                    return new Room(open, shell, false);
                }
                open.add(next);
                queue.add(next);
            }
        }
        return new Room(open, shell, !open.isEmpty());
    }
}
