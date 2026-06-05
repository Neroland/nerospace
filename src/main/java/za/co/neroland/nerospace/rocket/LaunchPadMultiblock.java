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

    /** Cap on the flood fill so a pathological field of pads can't stall a tick (≥ a sloppy 5x5 field). */
    private static final int MAX_PADS = 64;
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
        return fullSquareCorner(pads, 3) != null;
    }

    /**
     * Generalised footprint detection (LAUNCH_PAD_DESIGN.md §2): the min-corner of the first
     * complete aligned {@code size x size} square contained in {@code pads}, or {@code null}.
     */
    @Nullable
    public static BlockPos fullSquareCorner(Set<BlockPos> pads, int size) {
        if (pads.size() < size * size) {
            return null;
        }
        for (BlockPos corner : pads) {
            if (isSquareFrom(pads, corner, size)) {
                return corner;
            }
        }
        return null;
    }

    /**
     * Like {@link #fullSquareCorner(Set, int)} but the square must CONTAIN {@code pos} (XZ) — used
     * by launch gating so a rocket is grounded when the square it actually stands on degrades,
     * not kept alive by an intact square elsewhere in the connected cluster.
     */
    @Nullable
    public static BlockPos fullSquareCornerContaining(Set<BlockPos> pads, int size, BlockPos pos) {
        if (pads.size() < size * size) {
            return null;
        }
        for (BlockPos corner : pads) {
            if (pos.getX() >= corner.getX() && pos.getX() < corner.getX() + size
                    && pos.getZ() >= corner.getZ() && pos.getZ() < corner.getZ() + size
                    && isSquareFrom(pads, corner, size)) {
                return corner;
            }
        }
        return null;
    }

    /** Whether the {@code size x size} square with min-corner {@code corner} is contained in {@code pads}. */
    private static boolean isSquareFrom(Set<BlockPos> pads, BlockPos corner, int size) {
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                if (!pads.contains(new BlockPos(corner.getX() + dx, corner.getY(), corner.getZ() + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Whether the 3x3 with min-corner {@code corner} is entirely contained in {@code pads}. */
    private static boolean isThreeByThreeFrom(Set<BlockPos> pads, BlockPos corner) {
        return isSquareFrom(pads, corner, 3);
    }

    // --- Heavy Launch Complex (LAUNCH_PAD_DESIGN.md) -------------------------

    /**
     * The Heavy Launch Complex: a complete aligned 5x5 pad with at least one Launch Gantry module
     * on its border ring at pad level. Sign-off: the gantry is REQUIRED (a bare 5x5 is just a big
     * basic pad); a Heavy complex deploys/launches Tier 3 without the Station-Wall ring.
     */
    public static boolean isHeavyComplex(Level level, Set<BlockPos> pads) {
        BlockPos corner = fullSquareCorner(pads, 5);
        return corner != null && borderRingHas(level, corner, 5,
                state -> state.getBlock() instanceof LaunchGantryBlock);
    }

    /** {@link #isHeavyComplex} where the 5x5 must contain {@code pos} (launch gating). */
    public static boolean isHeavyComplexContaining(Level level, Set<BlockPos> pads, BlockPos pos) {
        BlockPos corner = fullSquareCornerContaining(pads, 5, pos);
        return corner != null && borderRingHas(level, corner, 5,
                state -> state.getBlock() instanceof LaunchGantryBlock);
    }

    /** {@link #hasStationWallRing} where the ringed 3x3 must contain {@code pos} (launch gating). */
    public static boolean hasStationWallRingAround(Level level, Set<BlockPos> pads, BlockPos pos) {
        for (BlockPos corner : pads) {
            if (pos.getX() >= corner.getX() && pos.getX() < corner.getX() + 3
                    && pos.getZ() >= corner.getZ() && pos.getZ() < corner.getZ() + 3
                    && isThreeByThreeFrom(pads, corner) && hasRingAt(level, corner)) {
                return true;
            }
        }
        return false;
    }

    /** Whether any cell of the border ring around the {@code size} square matches {@code predicate}. */
    public static boolean borderRingHas(Level level, BlockPos corner, int size,
            java.util.function.Predicate<BlockState> predicate) {
        for (int dx = -1; dx <= size; dx++) {
            for (int dz = -1; dz <= size; dz++) {
                if (dx != -1 && dx != size && dz != -1 && dz != size) {
                    continue; // interior — the pad itself
                }
                BlockPos pos = new BlockPos(corner.getX() + dx, corner.getY(), corner.getZ() + dz);
                if (predicate.test(level.getBlockState(pos))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tier 3 gating: whether some complete 3x3 in {@code pads} is ringed with Station Wall — the 16
     * border cells of the surrounding 5x5, at pad level. Checked against every candidate 3x3 corner
     * so an oversized pad field still qualifies if any aligned 3x3 carries a full ring.
     */
    public static boolean hasStationWallRing(Level level, Set<BlockPos> pads) {
        for (BlockPos corner : pads) {
            if (isThreeByThreeFrom(pads, corner) && hasRingAt(level, corner)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the 5x5 border around the 3x3 with min-corner {@code corner} is all Station Wall. */
    private static boolean hasRingAt(Level level, BlockPos corner) {
        for (int dx = -1; dx <= 3; dx++) {
            for (int dz = -1; dz <= 3; dz++) {
                if (dx != -1 && dx != 3 && dz != -1 && dz != 3) {
                    continue; // interior — the pad itself
                }
                BlockPos pos = new BlockPos(corner.getX() + dx, corner.getY(), corner.getZ() + dz);
                if (!level.getBlockState(pos).is(za.co.neroland.nerospace.registry.ModBlocks.STATION_WALL.get())) {
                    return false;
                }
            }
        }
        return true;
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
