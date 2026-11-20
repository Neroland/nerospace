package za.co.neroland.nerospace.world.planet.europa;

import java.lang.ref.WeakReference;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.block.rules.VentWarmthRules;
import za.co.neroland.nerospace.world.planet.ColdShelterRules;
import za.co.neroland.nerospace.world.planet.europa.block.HydrothermalVentBlock;
import za.co.neroland.nerospace.world.planet.europa.block.WarmBasaltBlock;

/**
 * Europa's positional cold shield for the hazard system: an {@code ACTIVE} Hydrothermal Vent within
 * {@link VentWarmthRules#RADIUS} (5) blocks, or warm Vent Basalt ({@code DISTANCE} 3 or less) within
 * {@link #BASALT_RADIUS} (2). One pass over the radius-5 sphere (515 cells, loaded chunks only) that stops at the
 * first hit. Reads block states only, so the same scan serves the server hazard check and the client HUD mirror.
 * Only called when cold would otherwise bite; the server side is cached by {@code ColdShelter}, the client side here.
 */
public final class EuropaWarmth {

    /** Reach of warm vent basalt, in blocks. */
    public static final int BASALT_RADIUS = 2;
    private static final int R = (int) VentWarmthRules.RADIUS;

    private static WeakReference<Level> clientLevel = new WeakReference<>(null);
    private static long clientPos;
    private static long clientBucket = Long.MIN_VALUE;
    private static boolean clientValue;

    private EuropaWarmth() {
    }

    /** Server lookup, called through {@code ColdShelter} only when cold would otherwise bite. False off Europa. */
    public static boolean shieldedFromCold(ServerLevel level, BlockPos pos) {
        return appliesTo(level.dimension().identifier()) && scan(level, pos);
    }

    /** Europa's dimension id ({@code ModDimensions.EUROPA_LEVEL}). */
    public static final net.minecraft.resources.Identifier EUROPA_DIMENSION =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("nerospace", "europa");

    /** The Europa-only gate: the shield is looked for in Europa's dimension and nowhere else. */
    public static boolean appliesTo(net.minecraft.resources.Identifier dimension) {
        return EUROPA_DIMENSION.equals(dimension);
    }

    /** What a scanned cell is, for {@link #cellShields}. */
    public enum Cell { VENT, BASALT, OTHER }

    /**
     * Pure scan predicate: does one cell shield the scan centre. A vent shields when active and within
     * {@link VentWarmthRules#RADIUS}; vent basalt when warm ({@code DISTANCE} 3 or less) and within
     * {@link #BASALT_RADIUS}; nothing else does.
     *
     * @param active          the vent's {@code ACTIVE} (ignored for other cells)
     * @param basaltDistance  the basalt's {@code DISTANCE} (ignored for other cells)
     * @param distanceSquared squared offset from the scan centre
     */
    public static boolean cellShields(Cell cell, boolean active, int basaltDistance, int distanceSquared) {
        return switch (cell) {
            case VENT -> VentWarmthRules.shielded(active, Math.sqrt(distanceSquared));
            case BASALT -> basaltReaches(distanceSquared)
                    && za.co.neroland.nerospace.block.rules.ConductRules.warm(basaltDistance);
            case OTHER -> false;
        };
    }

    /**
     * Whether the client cache answers: same level object (by identity; a new or unloaded level never hits), same
     * position, same 10-tick bucket.
     */
    public static boolean clientCacheHit(Object cachedLevel, Object level, long cachedPos, long cachedBucket,
            long pos, long bucket) {
        return ColdShelterRules.cacheHit(cachedLevel != null && cachedLevel == level, cachedPos, cachedBucket, pos,
                bucket);
    }

    /**
     * Client mirror for the HUD (reads block states only). False off Europa without a block read; otherwise cached
     * per position for {@link ColdShelterRules#bucket(long) one 10-tick bucket}.
     */
    public static boolean shieldedClient(Level level, BlockPos pos) {
        if (!appliesTo(level.dimension().identifier())) {
            return false;
        }
        long key = pos.asLong();
        long bucket = ColdShelterRules.bucket(level.getGameTime());
        if (clientCacheHit(clientLevel.get(), level, clientPos, clientBucket, key, bucket)) {
            return clientValue;
        }
        boolean value = scan(level, pos);
        clientLevel = new WeakReference<>(level);
        clientPos = key;
        clientBucket = bucket;
        clientValue = value;
        return value;
    }

    /** Whether a cell at squared distance {@code distanceSquared} can hold warm basalt that shields. */
    public static boolean basaltReaches(int distanceSquared) {
        return distanceSquared <= BASALT_RADIUS * BASALT_RADIUS;
    }

    private static boolean scan(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -R; dx <= R; dx++) {
            for (int dy = -R; dy <= R; dy++) {
                for (int dz = -R; dz <= R; dz++) {
                    int distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared > R * R) {
                        continue;
                    }
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState s = level.getBlockState(cursor);
                    if (s.is(EuropaContent.HYDROTHERMAL_VENT.get())
                            && cellShields(Cell.VENT, s.getValue(HydrothermalVentBlock.ACTIVE), 0, distanceSquared)) {
                        return true;
                    }
                    if (s.is(EuropaContent.VENT_BASALT.get()) && s.hasProperty(WarmBasaltBlock.DISTANCE)
                            && cellShields(Cell.BASALT, false, s.getValue(WarmBasaltBlock.DISTANCE),
                                    distanceSquared)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
