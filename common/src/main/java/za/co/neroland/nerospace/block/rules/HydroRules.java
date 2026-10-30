package za.co.neroland.nerospace.block.rules;

/** Hydroponics Tray: crops grow with a Hull Window within 3 above and inside an oxygen field. */
public final class HydroRules {

    public static final int WINDOW_RANGE = 3;

    private HydroRules() {
    }

    public static boolean canGrow(boolean windowWithin3Above, boolean inOxygen) {
        return windowWithin3Above && inOxygen;
    }

    /**
     * Class multiplier as a random-tick skip: growth proceeds when {@code roll < multiplier} ({@code roll} uniform
     * in [0,1)). 1.0 never skips.
     */
    public static boolean classAllows(double multiplier, double roll) {
        return multiplier >= 1.0 || roll < multiplier;
    }
}
