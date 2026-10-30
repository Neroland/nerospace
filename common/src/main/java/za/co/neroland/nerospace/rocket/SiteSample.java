package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;

/**
 * What the adapter read about one candidate Landing Pod site. Arrays are the 5×5 footprint in
 * row-major order (dz outer, dx inner, −2..+2); {@code headroomClear} is the 5×5×3 box (75 cells).
 *
 * @param floorSolid      per floor cell (y−1): full-collision block
 * @param floorForbidden  per floor cell: leaves, fluid, ice family, powder snow, magma or a block entity
 * @param headroomClear   per box cell (y..y+2): air/replaceable with no block entity
 * @param inStructure     a structure start intersects the box
 * @param nearestInaccessibleClaimDist Chebyshev distance to the nearest claim the rider cannot access
 * @param overlapsPad     the 5×5 overlaps a registered launch pad / dock box (hard rule)
 * @param podNearby       another standing Landing Pod node within 32 blocks
 * @param existingEmptyPod the site already holds an empty, unoccupied Landing Pod
 */
public record SiteSample(BlockPos site, boolean[] floorSolid, boolean[] floorForbidden, boolean[] headroomClear,
        boolean inStructure, int nearestInaccessibleClaimDist, boolean overlapsPad, boolean podNearby,
        boolean existingEmptyPod) {

    public static final int FOOTPRINT = 25;
    public static final int BOX = 75;

    /** A perfect site at {@code site} (tests and the climb path start from this). */
    public static SiteSample clear(BlockPos site) {
        return new SiteSample(site, fill(FOOTPRINT, true), fill(FOOTPRINT, false), fill(BOX, true), false,
                Integer.MAX_VALUE, false, false, false);
    }

    public static boolean[] fill(int n, boolean value) {
        boolean[] out = new boolean[n];
        java.util.Arrays.fill(out, value);
        return out;
    }

    /** Whether every headroom cell is clear. */
    public boolean boxClear() {
        for (boolean b : this.headroomClear) {
            if (!b) {
                return false;
            }
        }
        return true;
    }
}
