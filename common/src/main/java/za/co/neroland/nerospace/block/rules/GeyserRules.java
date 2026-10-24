package za.co.neroland.nerospace.block.rules;

/** Frozen Geyser (002 §1.4): every 160 ticks it vents, adding +1.2 dy to entities on it, with 60 ticks fall grace. */
public final class GeyserRules {

    public static final int INTERVAL_TICKS = 160;
    public static final double LAUNCH = 1.2;
    public static final int FALL_GRACE_TICKS = 60;
    public static final int CLOUD_PARTICLES = 12;

    private GeyserRules() {
    }

    public static double launchVelocity(boolean charged, double currentDy) {
        return charged ? currentDy + LAUNCH : currentDy;
    }
}
