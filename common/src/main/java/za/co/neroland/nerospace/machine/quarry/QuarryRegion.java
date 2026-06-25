package za.co.neroland.nerospace.machine.quarry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

/**
 * The rectangular footprint a quarry mines, derived from its landmarks (3 forming an L). Landmarks
 * "project" along the four horizontal axes; a flood-fill over those links collects the cluster and its
 * X/Z bounding box becomes the mined rectangle. The reference plane {@link #refY()} is the landmarks'
 * Y; mining runs from {@code refY - 1} down to the world floor. Immutable; persisted in NBT.
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

    public int columns() {
        return width() * length();
    }

    public boolean containsColumn(int x, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
    }

    public boolean isPerimeter(int x, int z) {
        return x == this.minX || x == this.maxX || z == this.minZ || z == this.maxZ;
    }

    public boolean acceptsController(BlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();
        if (pos.getY() != this.refY) {
            return false;
        }
        if (containsColumn(x, z)) {
            return isPerimeter(x, z);
        }
        boolean besideWestOrEast = (x == this.minX - 1 || x == this.maxX + 1)
                && z >= this.minZ && z <= this.maxZ;
        boolean besideNorthOrSouth = (z == this.minZ - 1 || z == this.maxZ + 1)
                && x >= this.minX && x <= this.maxX;
        return besideWestOrEast || besideNorthOrSouth;
    }

    public List<BlockPos> framePositions() {
        List<BlockPos> out = new ArrayList<>();
        for (int x = this.minX; x <= this.maxX; x++) {
            out.add(new BlockPos(x, this.refY, this.minZ));
        }
        for (int z = this.minZ + 1; z <= this.maxZ; z++) {
            out.add(new BlockPos(this.maxX, this.refY, z));
        }
        if (this.maxZ > this.minZ) {
            for (int x = this.maxX - 1; x >= this.minX; x--) {
                out.add(new BlockPos(x, this.refY, this.maxZ));
            }
        }
        if (this.maxX > this.minX) {
            for (int z = this.maxZ - 1; z > this.minZ; z--) {
                out.add(new BlockPos(this.minX, this.refY, z));
            }
        }
        return out;
    }

    public BlockPos columnPos(int index, int y) {
        int w = width();
        int dx = index % w;
        int dz = index / w;
        return new BlockPos(this.minX + dx, y, this.minZ + dz);
    }

    // --- Discovery from landmarks ------------------------------------------------

    private static final int MAX_LANDMARKS = 16;

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
            BlockPos pos = java.util.Objects.requireNonNull(queue.poll());
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                Direction checkedDir = java.util.Objects.requireNonNull(dir);
                BlockPos linked = projectToLandmark(level, pos, checkedDir, maxSide);
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
        if (cluster.size() < 3 || w < 2 || l < 2 || w > maxSide || l > maxSide) {
            return null;
        }
        return new QuarryRegion(minX, minZ, maxX, maxZ, refY);
    }

    @Nullable
    public static QuarryRegion findClaim(Level level, BlockPos origin, int maxSide) {
        QuarryRegion best = null;
        int bestScore = Integer.MAX_VALUE;
        int bestArea = Integer.MAX_VALUE;
        List<BlockPos> landmarks = collectNearbyLandmarks(level, origin, maxSide);
        Map<Integer, List<BlockPos>> byX = new HashMap<>();
        Map<Integer, List<BlockPos>> byZ = new HashMap<>();
        for (BlockPos pos : landmarks) {
            byX.computeIfAbsent(pos.getX(), ignored -> new ArrayList<>()).add(pos);
            byZ.computeIfAbsent(pos.getZ(), ignored -> new ArrayList<>()).add(pos);
        }
        for (BlockPos corner : landmarks) {
            List<BlockPos> xMates = byZ.getOrDefault(corner.getZ(), List.of());
            List<BlockPos> zMates = byX.getOrDefault(corner.getX(), List.of());
            for (BlockPos xMate : xMates) {
                if (xMate.getX() == corner.getX()) {
                    continue;
                }
                for (BlockPos zMate : zMates) {
                    if (zMate.getZ() == corner.getZ()) {
                        continue;
                    }
                    QuarryRegion found = new QuarryRegion(corner.getX(), corner.getZ(),
                            xMate.getX(), zMate.getZ(), origin.getY());
                    int w = found.width();
                    int l = found.length();
                    if (w < 2 || l < 2 || w > maxSide || l > maxSide || !found.acceptsController(origin)) {
                        continue;
                    }
                    int score = found.controllerDistance(origin);
                    int area = w * l;
                    if (score < bestScore || (score == bestScore && area < bestArea)) {
                        best = found;
                        bestScore = score;
                        bestArea = area;
                    }
                }
            }
        }
        return best;
    }

    private static List<BlockPos> collectNearbyLandmarks(Level level, BlockPos origin, int maxSide) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -maxSide; dx <= maxSide; dx++) {
            for (int dz = -maxSide; dz <= maxSide; dz++) {
                cursor.set(origin.getX() + dx, origin.getY(), origin.getZ() + dz);
                if (isLandmark(level, cursor)) {
                    out.add(cursor.immutable());
                }
            }
        }
        return out;
    }

    private int controllerDistance(BlockPos pos) {
        int dx = 0;
        if (pos.getX() < this.minX) {
            dx = this.minX - pos.getX();
        } else if (pos.getX() > this.maxX) {
            dx = pos.getX() - this.maxX;
        }
        int dz = 0;
        if (pos.getZ() < this.minZ) {
            dz = this.minZ - pos.getZ();
        } else if (pos.getZ() > this.maxZ) {
            dz = pos.getZ() - this.maxZ;
        }
        return dx + dz;
    }

    @Nullable
    private static BlockPos projectToLandmark(Level level, BlockPos from, Direction dir, int range) {
        BlockPos.MutableBlockPos cursor = from.mutable();
        for (int step = 1; step <= range; step++) {
            cursor.move(za.co.neroland.nerospace.NerospaceCommon.requireNonNull(dir));
            if (isLandmark(level, cursor)) {
                return cursor.immutable();
            }
        }
        return null;
    }

    private static boolean isLandmark(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(za.co.neroland.nerospace.NerospaceCommon.requireNonNull(pos));
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

    public static QuarryRegion load(ValueInput input) {
        return new QuarryRegion(
                input.getIntOr("MinX", 0),
                input.getIntOr("MinZ", 0),
                input.getIntOr("MaxX", 0),
                input.getIntOr("MaxZ", 0),
                input.getIntOr("RefY", 0));
    }
}
