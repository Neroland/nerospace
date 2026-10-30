package za.co.neroland.nerospace.world;

import net.minecraft.core.BlockPos;

/**
 * Region-of-interest spacing + density cap. The world is divided into square <b>region cells</b>
 * ({@link #CELL_CHUNKS}×{@link #CELL_CHUNKS} chunks). Each cell deterministically gets <b>at most one</b>
 * ROI — and which kind is chosen by a hash of the cell, with weights that keep the rarer ones rare. The ROI
 * sits at a deterministic "anchor" chunk inside the cell.
 *
 * <p>Per planet, the hash is salted with {@code PlanetDefinition.structureSalt()} so worlds that share structure
 * types do not share cell assignments. Salt 0 (Greenxertz, Cindara, Glacira) reproduces the 1.6.0 hash exactly,
 * so existing worlds' structure positions never move. Village kinds (hamlet / ruin / mega-city) and the Mars kinds
 * (rover wreck / derelict habitat) use disjoint roll bands; only the kinds a planet's biomes list can appear.</p>
 */
public final class StructureSpacing {

    /** Size of a region cell in chunks (16 chunks ≈ 256 blocks). One ROI per cell, max. */
    public static final int CELL_CHUNKS = 16;

    /** Not persisted; append freely. */
    public enum Roi { NONE, HAMLET, RUIN, MEGA_CITY, ROVER_WRECK, DERELICT_HABITAT }

    private StructureSpacing() {
    }

    /** The 1.6.0 entry point: salt 0. */
    public static boolean shouldPlace(BlockPos origin, Roi mine) {
        return shouldPlace(origin, mine, 0L);
    }

    /**
     * @return true only when {@code origin}'s chunk is the anchor of a region cell whose assigned ROI
     *         is {@code mine}. Call this first in a feature's {@code place} and bail out if false.
     */
    public static boolean shouldPlace(BlockPos origin, Roi mine, long salt) {
        return shouldPlaceChunk(origin.getX() >> 4, origin.getZ() >> 4, mine, salt);
    }

    /** Pure form of {@link #shouldPlace(BlockPos, Roi, long)} on chunk coordinates. */
    public static boolean shouldPlaceChunk(int cx, int cz, Roi mine, long salt) {
        int rx = Math.floorDiv(cx, CELL_CHUNKS);
        int rz = Math.floorDiv(cz, CELL_CHUNKS);
        long h = mix(rx, rz, salt);
        if (roiFor(h) != mine) {
            return false;
        }
        int span = Math.max(1, CELL_CHUNKS - 4);
        int ax = 2 + Math.floorMod(h >>> 8, span);
        int az = 2 + Math.floorMod(h >>> 24, span);
        return Math.floorMod(cx, CELL_CHUNKS) == ax && Math.floorMod(cz, CELL_CHUNKS) == az;
    }

    /** The kind assigned to a cell hash. The first three bands are the 1.6.0 bands, unchanged. */
    static Roi roiFor(long h) {
        int roll = (int) Math.floorMod(h, 100L);
        if (roll < 26) {
            return Roi.HAMLET;            // 26%
        } else if (roll < 34) {
            return Roi.RUIN;              // 8%
        } else if (roll < 36) {
            return Roi.MEGA_CITY;         // 2%
        } else if (roll < 48) {
            return Roi.ROVER_WRECK;       // 12% (Mars only lists it)
        } else if (roll < 54) {
            return Roi.DERELICT_HABITAT;  // 6% (Mars only lists it)
        }
        return Roi.NONE;
    }

    static long mix(int x, int z, long salt) {
        // salt == 0 is bit-for-bit the 1.6.0 hash.
        long h = x * 341873128712L + z * 132897987541L + 0x9E3779B97F4A7C15L + salt * 0x632BE59BD9B4E019L;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return h;
    }
}
