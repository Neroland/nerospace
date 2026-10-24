package za.co.neroland.nerospace.block.rules;

/**
 * Methane Ice (002 §1.4, §11.5): explodes (power 2, no block damage unless configured) when a flame is within 2
 * blocks; scans a 5×5×5 every 40 ticks only while armed; at most 4 chained explosions per chunk per 100 ticks.
 */
public final class MethaneRules {

    public static final int IGNITE_DISTANCE = 2;
    public static final int SCAN_RADIUS = 2;
    public static final int SCAN_INTERVAL_TICKS = 40;
    public static final float POWER = 2.0F;
    public static final int CHAIN_CAP = 4;
    public static final int CHAIN_WINDOW_TICKS = 100;

    private MethaneRules() {
    }

    /** {@code nearestFlameDistance} is Chebyshev distance; negative = none found. */
    public static boolean ignites(int nearestFlameDistance, boolean chainBudgetLeft) {
        return nearestFlameDistance >= 0 && nearestFlameDistance <= IGNITE_DISTANCE && chainBudgetLeft;
    }

    public static boolean chainBudgetLeft(int explosionsThisWindow, int configuredCap) {
        return explosionsThisWindow < Caps.lower(CHAIN_CAP, configuredCap);
    }
}
