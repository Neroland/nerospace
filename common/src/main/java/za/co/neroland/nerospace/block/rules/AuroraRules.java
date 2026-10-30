package za.co.neroland.nerospace.block.rules;

/** Aurora Stone: lit (light 8) only while the planet daylight is below 0.25; re-read every 200 ticks. */
public final class AuroraRules {

    public static final double NIGHT_BELOW = 0.25;
    public static final int INTERVAL_TICKS = 200;
    public static final int LIGHT = 8;

    private AuroraRules() {
    }

    public static boolean lit(double daylight) {
        return daylight < NIGHT_BELOW;
    }
}
