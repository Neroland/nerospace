package za.co.neroland.nerospace.world;

import net.minecraft.core.BlockPos;

/**
 * Region-of-interest spacing + density cap. The world is divided into square <b>region cells</b>
 * ({@link #CELL_CHUNKS}×{@link #CELL_CHUNKS} chunks). Each cell deterministically gets <b>at most one</b>
 * ROI — and which kind (hamlet / ruin / mega-city, or nothing) is chosen by a hash of the cell, with
 * weights that keep the rarer ones rare. The ROI sits at a deterministic "anchor" chunk inside the cell.
 */
public final class StructureSpacing {

    /** Size of a region cell in chunks (16 chunks ≈ 256 blocks). One ROI per cell, max. */
    public static final int CELL_CHUNKS = 16;

    public enum Roi { NONE, HAMLET, RUIN, MEGA_CITY }

    private StructureSpacing() {
    }

    /**
     * @return true only when {@code origin}'s chunk is the anchor of a region cell whose assigned ROI
     *         is {@code mine}. Call this first in a feature's {@code place} and bail out if false.
     */
    public static boolean shouldPlace(BlockPos origin, Roi mine) {
        int cx = origin.getX() >> 4;
        int cz = origin.getZ() >> 4;
        int rx = Math.floorDiv(cx, CELL_CHUNKS);
        int rz = Math.floorDiv(cz, CELL_CHUNKS);
        long h = mix(rx, rz);

        int roll = (int) Math.floorMod(h, 100L);
        Roi type;
        if (roll < 26) {
            type = Roi.HAMLET;       // 26%
        } else if (roll < 34) {
            type = Roi.RUIN;         // 8%
        } else if (roll < 36) {
            type = Roi.MEGA_CITY;    // 2%
        } else {
            type = Roi.NONE;         // 64% empty
        }
        if (type != mine) {
            return false;
        }
        int span = Math.max(1, CELL_CHUNKS - 4);
        int ax = 2 + (int) Math.floorMod(h >>> 8, span);
        int az = 2 + (int) Math.floorMod(h >>> 24, span);
        return Math.floorMod(cx, CELL_CHUNKS) == ax && Math.floorMod(cz, CELL_CHUNKS) == az;
    }

    private static long mix(int x, int z) {
        long h = x * 341873128712L + z * 132897987541L + 0x9E3779B97F4A7C15L;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return h;
    }
}
