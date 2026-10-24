package za.co.neroland.nerospace.block.rules;

/** Helium-3 Pocket yield gate (002 §1.1): silk touch or an adjacent Gas Tank keeps the full yield. */
public final class GasPocketRules {

    public static final int BASE_YIELD = 2;
    /** Client leak particles and their duration. */
    public static final int LEAK_PARTICLES = 20;
    public static final int LEAK_TICKS = 60;

    private GasPocketRules() {
    }

    public static int yield(boolean silkTouch, boolean gasTankAdjacent, int baseYield) {
        if (silkTouch || gasTankAdjacent) {
            return baseYield;
        }
        return Math.max(1, baseYield / 2);
    }

    public static boolean leaks(boolean silkTouch, boolean gasTankAdjacent) {
        return !silkTouch && !gasTankAdjacent;
    }
}
