package za.co.neroland.nerospace.rocket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Geometry helpers for the launch-pad multiblock (Phase 8a). A "launch pad" in survival is no longer
 * a single block: any horizontally-connected cluster of {@link RocketLaunchPadBlock} blocks (sharing
 * one Y level) forms the pad footprint a rocket is fuelled on. A complete {@code 3x3} square is the
 * canonical pad and is recognised so adjacent machinery (the Fuel Tank) can reward it with a faster
 * fuel feed and, later, scale rocket size to the footprint.
 *
 * <p>This class is pure geometry: it never mutates the world, so it is safe to call from a block
 * entity tick.</p>
 */
public final class LaunchPadMultiblock {

    /** Cap on the flood fill so a pathological field of pads can't stall a tick. */
    private static final int MAX_PADS = 25;
    /** How far above the pad surface a rocket's feet may be and still count as "on the pad". */
    private static final double SCAN_HEIGHT = 8.0D;

    private LaunchPadMultiblock() {
    }

    /** @return the first horizontally-adjacent launch-pad position to {@code origin}, or {@code null}. */
    @Nullable
    public static BlockPos adjacentPad(Level level, BlockPos origin) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = origin.relative(dir);
            if (isPad(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Flood-fills the horizontally-connected pad cluster containing {@code start} (same Y level),
     * capped at {@link #MAX_PADS}. Returns an empty set if {@code start} is not a pad.
     */
    public static Set<BlockPos> connectedPads(Level level, BlockPos start) {
        Set<BlockPos> found = new HashSet<>();
        if (!isPad(level, start)) {
            return found;
        }
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        found.add(start.immutable());
        while (!queue.isEmpty() && found.size() < MAX_PADS) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(dir);
                if (!found.contains(next) && isPad(level, next)) {
                    BlockPos immutable = next.immutable();
                    found.add(immutable);
                    queue.add(immutable);
                }
            }
        }
        return found;
    }

    /** Whether {@code pads} contains a complete aligned 3x3 square (the canonical full pad). */
    public static boolean isFullThreeByThree(Set<BlockPos> pads) {
        if (pads.size() < 9) {
            return false;
        }
        for (BlockPos corner : pads) {
            boolean complete = true;
            for (int dx = 0; dx < 3 && complete; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    if (!pads.contains(new BlockPos(corner.getX() + dx, corner.getY(), corner.getZ() + dz))) {
                        complete = false;
                        break;
                    }
                }
            }
            if (complete) {
                return true;
            }
        }
        return false;
    }

    /** The first {@link RocketEntity} standing on top of any pad in {@code pads}, or {@code null}. */
    @Nullable
    public static RocketEntity rocketAbove(Level level, Set<BlockPos> pads) {
        if (pads.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int padY = pads.iterator().next().getY();
        for (BlockPos pad : pads) {
            minX = Math.min(minX, pad.getX());
            minZ = Math.min(minZ, pad.getZ());
            maxX = Math.max(maxX, pad.getX());
            maxZ = Math.max(maxZ, pad.getZ());
        }
        AABB box = new AABB(minX, padY + 0.1D, minZ, maxX + 1.0D, padY + 1.0D + SCAN_HEIGHT, maxZ + 1.0D);
        List<RocketEntity> rockets = level.getEntitiesOfClass(RocketEntity.class, box);
        for (RocketEntity rocket : rockets) {
            if (!rocket.isLaunching()) {
                return rocket;
            }
        }
        return null;
    }

    private static boolean isPad(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof RocketLaunchPadBlock;
    }
}
