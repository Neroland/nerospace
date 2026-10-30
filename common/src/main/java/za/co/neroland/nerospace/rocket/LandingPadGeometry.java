package za.co.neroland.nerospace.rocket;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

/**
 * Pure pad-geometry helpers: the centre of the 5×5 or 3×3 square containing a node's position,
 * and the pad's front from which sides of the square open onto air.
 */
public final class LandingPadGeometry {

    /** Facing order for {@link #facingOf}: index 0..3 of the {@code apronOpen} array. */
    public static final Direction[] ORDER = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };

    private LandingPadGeometry() {
    }

    /** The centre of the full 5×5 (else 3×3) square in {@code cluster} containing {@code pos}; else {@code pos}. */
    public static BlockPos centreOf(Set<BlockPos> cluster, BlockPos pos) {
        @Nullable BlockPos corner = cornerContaining(cluster, 5, pos);
        int size = 5;
        if (corner == null) {
            corner = cornerContaining(cluster, 3, pos);
            size = 3;
        }
        if (corner == null) {
            return pos.immutable();
        }
        return new BlockPos(corner.getX() + size / 2, corner.getY(), corner.getZ() + size / 2);
    }

    /** Half-width of the square {@link #centreOf} found (2 for 5×5, 1 for 3×3, 0 for a lone block). */
    public static int halfSize(Set<BlockPos> cluster, BlockPos pos) {
        if (cornerContaining(cluster, 5, pos) != null) {
            return 2;
        }
        return cornerContaining(cluster, 3, pos) != null ? 1 : 0;
    }

    /**
     * The first side (in {@link #ORDER}) whose apron is open while the opposite side is not; ties/none →
     * {@link Direction#SOUTH}. {@code apronOpen} is indexed like {@link #ORDER}.
     */
    public static Direction facingOf(boolean[] apronOpen) {
        for (int i = 0; i < 4; i++) {
            if (apronOpen[i] && !apronOpen[(i + 2) % 4]) {
                return ORDER[i];
            }
        }
        return Direction.SOUTH;
    }

    @Nullable
    static BlockPos cornerContaining(Set<BlockPos> pads, int size, BlockPos pos) {
        if (pads.size() < size * size) {
            return null;
        }
        for (int cx = pos.getX() - size + 1; cx <= pos.getX(); cx++) {
            for (int cz = pos.getZ() - size + 1; cz <= pos.getZ(); cz++) {
                BlockPos corner = new BlockPos(cx, pos.getY(), cz);
                if (square(pads, corner, size)) {
                    return corner;
                }
            }
        }
        return null;
    }

    private static boolean square(Set<BlockPos> pads, BlockPos corner, int size) {
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                if (!pads.contains(new BlockPos(corner.getX() + dx, corner.getY(), corner.getZ() + dz))) {
                    return false;
                }
            }
        }
        return true;
    }
}
