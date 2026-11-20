package za.co.neroland.nerospace.block.rules;

/**
 * Hydrothermal Vent warmth: a vent with liquid (water or brine) directly above it is active, glows and switches the
 * cold hazard off within {@link #RADIUS} blocks. A dry vent does nothing.
 */
public final class VentWarmthRules {

    /** Reach of an active vent's warmth, in blocks. */
    public static final double RADIUS = 5.0;
    /** Light of an active vent. */
    public static final int ACTIVE_LIGHT = 7;

    private VentWarmthRules() {
    }

    /** A vent is active exactly when the cell above it holds water or brine. */
    public static boolean active(boolean liquidAbove) {
        return liquidAbove;
    }

    public static boolean shielded(boolean active, double distance) {
        return active && distance <= RADIUS;
    }

    public static int light(boolean active) {
        return active ? ACTIVE_LIGHT : 0;
    }
}
