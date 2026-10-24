package za.co.neroland.nerospace.block.rules;

/** Lunar Basalt solar sinter (002 §1.1): only player-placed basalt in Luna sunlight. */
public final class SolarSinterRules {

    /** Random-tick odds: 1 in this many. */
    public static final int ONE_IN = 6;
    /** Minimum planet daylight to sinter. */
    public static final double MIN_DAYLIGHT = 0.5;

    private SolarSinterRules() {
    }

    public static boolean canSinter(boolean playerPlaced, boolean skyExposed, double daylight, boolean onLuna) {
        return playerPlaced && skyExposed && onLuna && daylight >= MIN_DAYLIGHT;
    }

    /** {@code roll} uniform in [0, {@link #ONE_IN}); 0 sinters. */
    public static boolean rolled(int roll) {
        return roll == 0;
    }
}
