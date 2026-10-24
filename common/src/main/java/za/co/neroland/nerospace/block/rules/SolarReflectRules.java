package za.co.neroland.nerospace.block.rules;

/** Anorthosite reflector (002 §1.1, §10.10): +25 % for a full 3×3 under a ground panel; never stacks with a wing. */
public final class SolarReflectRules {

    public static final int FOOTPRINT = 9;
    public static final double BONUS = 1.25;
    /** Panel re-checks its footprint this often (cached in the block entity). */
    public static final int CHECK_INTERVAL_TICKS = 40;

    private SolarReflectRules() {
    }

    public static double reflectMultiplier(int anorthositeCountUnder9) {
        return anorthositeCountUnder9 >= FOOTPRINT ? BONUS : 1.0;
    }

    /** A Solar Wing never takes the reflector bonus. */
    public static double reflectMultiplier(int anorthositeCountUnder9, boolean isSolarWing) {
        return isSolarWing ? 1.0 : reflectMultiplier(anorthositeCountUnder9);
    }
}
