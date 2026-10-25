package za.co.neroland.nerospace.block.rules;

/**
 * Self-rescheduling blocks (Aurora Stone clock, Frozen Geyser vent) start their schedule in {@code onPlace}, which
 * worldgen never calls. Their bases therefore also bootstrap on a random tick: when no tick is pending, schedule one;
 * when one is pending, do nothing (the running schedule is never doubled or shifted).
 */
public final class TickBootstrapRules {

    private TickBootstrapRules() {
    }

    /** A random tick starts the schedule only when none is pending. */
    public static boolean shouldBootstrap(boolean tickPending) {
        return !tickPending;
    }

    /**
     * A gravity-override block (Gravity Shard Block, Gravity Plate) registers its override on a random tick only when
     * none is stored for its position, so a worldgen-placed block starts working and a placed one is untouched.
     */
    public static boolean shouldRegisterOverride(boolean registered) {
        return !registered;
    }

    /** First delay for a clock-lit block bootstrapped by a random tick (random ticks already spread the phase). */
    public static int clockLitDelay() {
        return 1;
    }

    /** First delay for a geyser bootstrapped by a random tick: 1 .. half an interval, from {@code roll} in [0, half). */
    public static int geyserDelay(int roll) {
        int half = GeyserRules.INTERVAL_TICKS / 2;
        return 1 + Math.floorMod(roll, half);
    }
}
