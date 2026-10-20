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

    public enum Problem
    {
        NONE,
        /**
         * The open blocks reach the outside (sky) of a room that must be closed.
         */
        NOT_CLOSED,
        /**
         * More open blocks than the rules allow: the room is too big, or leaks outside under trees or overhangs.
         */
        TOO_BIG,
        /**
         * No covered open block next to the hut: it is not under a roof.
         */
        NOT_COVERED
    }

    /**
     * @param open    covered blocks a player could walk through (air, torches...), inside the room.
     * @param shell   blocks touching the open ones: floor, walls, roof, furniture, doors. Includes the hut.
     * @param problem why the room cannot be used, or {@link Problem#NONE}.
     */
    public record Room(Set<BlockPos> open, Set<BlockPos> shell, Problem problem)
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

    /**
     * @param closed when false, openings (a doorway, a window without glass) are allowed: open blocks under the sky are
     *               the outside and end the room there, instead of failing the scan.
     */
    public static Room scan(final Level level, final BlockPos hut, final int maxVolume, final boolean closed)
    {
        final Set<BlockPos> open = new HashSet<>();
        final Set<BlockPos> shell = new HashSet<>();
        final Set<BlockPos> outside = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        shell.add(hut);
        queue.add(hut);

        while (!queue.isEmpty())
        {
            final BlockPos current = queue.poll();
            for (final Direction direction : Direction.values())
            {
                final BlockPos next = current.relative(direction);
                if (open.contains(next) || shell.contains(next) || outside.contains(next))
                {
                    continue;
                }
                if (!level.isLoaded(next))
                {
                    return new Room(open, shell, Problem.TOO_BIG);
                }
                if (!level.getBlockState(next).getCollisionShape(level, next).isEmpty())
                {
                    shell.add(next);
                    continue;
                }
                if (level.canSeeSky(next))
                {
                    if (closed)
                    {
                        return new Room(open, shell, Problem.NOT_CLOSED);
                    }
                    outside.add(next);
                    continue;
                }
                if (open.size() >= maxVolume)
                {
                    return new Room(open, shell, Problem.TOO_BIG);
                }
                open.add(next);
                queue.add(next);
            }
        }
        return new Room(open, shell, open.isEmpty() ? Problem.NOT_COVERED : Problem.NONE);
    }
}
