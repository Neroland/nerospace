package za.co.neroland.nerospace.block.rules;

/**
 * Tube Mat bloom: a submerged mat with an active vent within 6 blocks spreads to a neighbouring patch of vent basalt,
 * until {@link #CAP} mats stand in the scanned box around it. A dry mat, or one with no vent near, never spreads.
 */
public final class VentBloomRules {

    /** Mats in the scanned box at which spreading stops. */
    public static final int CAP = 24;
    /** Share of random ticks that attempt a spread (the scan runs only on those). */
    public static final float SPREAD_CHANCE = 0.25F;
    /** Horizontal reach of the vent and cap scan. */
    public static final int RANGE = 6;
    /** Vertical band of the scan, relative to the mat. */
    public static final int BAND_DOWN = 2;
    public static final int BAND_UP = 1;
    /** Light the mat gives. */
    public static final int LIGHT = 7;

    private VentBloomRules() {
    }

    /** Whether this random tick attempts a spread, from a roll in {@code [0, 1)}. */
    public static boolean attempts(float roll) {
        return roll < SPREAD_CHANCE;
    }

    public static boolean maySpread(boolean submerged, boolean ventInRange, int matsInBox) {
        return submerged && ventInRange && matsInBox < CAP;
    }

    /** Block reads of one scan: (2 x RANGE + 1) squared, times the band height. */
    public static int scanReads() {
        int side = 2 * RANGE + 1;
        return side * side * (BAND_DOWN + BAND_UP + 1);
    }
}
