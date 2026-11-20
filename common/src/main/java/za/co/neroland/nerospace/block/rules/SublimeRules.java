package za.co.neroland.nerospace.block.rules;

/**
 * Deuterium Ice sublimation: mined with a heat source within 4 blocks (a cube, so 9 x 9 x 9 cells) the ice boils off
 * and drops nothing. Silk Touch lifts the block whole whatever is near.
 */
public final class SublimeRules {

    /** Reach of the heat check on each axis. */
    public static final int RANGE = 4;

    private SublimeRules() {
    }

    /** Whether the block gives its shards: only with no heat source in range. */
    public static boolean yields(boolean heatWithin4) {
        return !heatWithin4;
    }

    /** Whether an offset is inside the checked cube. */
    public static boolean within(int dx, int dy, int dz) {
        return Math.abs(dx) <= RANGE && Math.abs(dy) <= RANGE && Math.abs(dz) <= RANGE;
    }

    /** Block reads of one full check. */
    public static int scanReads() {
        int side = 2 * RANGE + 1;
        return side * side * side;
    }
}
