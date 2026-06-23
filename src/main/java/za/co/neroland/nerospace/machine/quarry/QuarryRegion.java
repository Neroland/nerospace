package za.co.neroland.nerospace.machine.quarry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

/**
 * The rectangular footprint a quarry mines, derived from its landmarks (MINER_DESIGN — 3 landmarks
 * forming an L). Landmarks "project" along the four horizontal axes (BuildCraft-style), so two
 * landmarks are linked when they share a row or column at the same Y within range; a flood-fill over
 * those links collects the cluster and its X/Z bounding box becomes the mined rectangle. The
 * reference plane {@link #refY()} is the landmarks' Y; mining runs from {@code refY - 1} down to the
 * world floor.
 *
 * <p>Immutable; persisted in the controller's NBT via {@link #save}/{@link #load}.</p>
 */
public final class QuarryRegion {

    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final int refY;

    public QuarryRegion(int minX, int minZ, int maxX, int maxZ, int refY) {
        this.minX = Math.min(minX, maxX);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxZ = Math.max(minZ, maxZ);
        this.refY = refY;
    }

    public int minX() {
        return this.minX;
    }

    public int minZ() {
        return this.minZ;
    }

    public int maxX() {
        return this.maxX;
    }

    public int maxZ() {
        return this.maxZ;
    }

    public int refY() {
        return this.refY;
    }

    public int width() {
        return this.maxX - this.minX + 1;
    }

    public int length() {
        return this.maxZ - this.minZ + 1;
    }

    /** Number of columns (interior cells) the quarry sweeps per layer. */
    public int columns() {
        return width() * length();
    }

    public boolean containsColumn(int x, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
    }

    /** Whether {@code (x,z)} sits on the rectangle's perimeter (where the frame ring is built). */
    public boolean isPerimeter(int x, int z) {
        return x == this.minX || x == this.maxX || z == this.minZ || z == this.maxZ;
    }

    /** The perimeter cells (at {@link #refY}) the frame ring occupies. */
    public List<BlockPos> framePositions() {
        List<BlockPos> out = new ArrayList<>();
        for (int x = this.minX; x <= this.maxX; x++) {
            for (int z = this.minZ; z <= this.maxZ; z++) {
                if (isPerimeter(x, z)) {
                    out.add(new BlockPos(x, this.refY, z));
                }
            }
        }
        return out;
    }

    /** The mining target at linear column {@code index} (row-major) and layer {@code y}. */
    public BlockPos columnPos(int index, int y) {
        int w = width();
        int dx = index % w;
        int dz = index / w;
        return new BlockPos(this.minX + dx, y, this.minZ + dz);
    }

    // --- Discovery from landmarks ------------------------------------------------

    /** Maximum landmarks gathered by a single scan (a sane safety cap). */
    private static final int MAX_LANDMARKS = 16;

    /**
     * Build a region from the landmark cluster reachable from {@code seed}, validated against
     * {@code maxSide}. Returns {@code null} if the cluster is degenerate (a single landmark) or the
     * span exceeds the tier's area cap.
     */
    @Nullable
    public static QuarryRegion fromLandmarks(Level level, BlockPos seed, int maxSide) {
        if (!isLandmark(level, seed)) {
            return null;
        }
        Set<BlockPos> cluster = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        cluster.add(seed.immutable());
        queue.add(seed.immutable());
        int refY = seed.getY();

        while (!queue.isEmpty() && cluster.size() < MAX_LANDMARKS) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos linked = projectToLandmark(level, pos, dir, maxSide);
                if (linked != null && cluster.add(linked)) {
                    queue.add(linked);
                }
            }
        }

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : cluster) {
            minX = Math.min(minX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        int w = maxX - minX + 1;
        int l = maxZ - minZ + 1;
        if (cluster.size() < 2 || w < 2 || l < 2 || w > maxSide || l > maxSide) {
            return null;
        }
        return new QuarryRegion(minX, minZ, maxX, maxZ, refY);
    }

    /**
     * Find a landmark to seed a scan from, near {@code origin} (the controller): the nearest landmark
     * along any horizontal axis within {@code range}, or {@code null}.
     */
    @Nullable
    public static BlockPos findNearbyLandmark(Level level, BlockPos origin, int range) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos found = projectToLandmark(level, origin, dir, range);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Scan along {@code dir} up to {@code range} for the next landmark sharing this row/column. */
    @Nullable
    private static BlockPos projectToLandmark(Level level, BlockPos from, Direction dir, int range) {
        BlockPos.MutableBlockPos cursor = from.mutable();
        for (int step = 1; step <= range; step++) {
            cursor.move(dir);
            if (isLandmark(level, cursor)) {
                return cursor.immutable();
            }
        }
        return null;
    }

    private static boolean isLandmark(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof QuarryLandmarkBlock;
    }

    // --- Persistence -------------------------------------------------------------

    public void save(ValueOutput output) {
        output.putInt("MinX", this.minX);
        output.putInt("MinZ", this.minZ);
        output.putInt("MaxX", this.maxX);
        output.putInt("MaxZ", this.maxZ);
        output.putInt("RefY", this.refY);
    }

    /** Reads a region from a child written by {@link #save}; the caller gates on whether one exists. */
    public static QuarryRegion load(ValueInput input) {
        return new QuarryRegion(
                input.getIntOr("MinX", 0),
                input.getIntOr("MinZ", 0),
                input.getIntOr("MaxX", 0),
                input.getIntOr("MaxZ", 0),
                input.getIntOr("RefY", 0));
    }
}
