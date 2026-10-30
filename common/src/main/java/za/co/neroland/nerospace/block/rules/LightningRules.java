package za.co.neroland.nerospace.block.rules;

/** Basalt Spire: ember strikes on a {@code nerospace:lightning_safe} top are cancelled. */
public final class LightningRules {

    public static final int INTERVAL_TICKS = 200;
    public static final double MIN_INTENSITY = 0.7;

    private LightningRules() {
    }

    public static boolean strikeAllowed(boolean topIsSafe) {
        return !topIsSafe;
    }

    public static boolean emberStorm(boolean ashFall, double intensity) {
        return ashFall && intensity >= MIN_INTENSITY;
    }
}
