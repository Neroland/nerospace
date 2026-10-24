package za.co.neroland.nerospace.block.rules;

/** Hematite magnet (002 §1.2): drops fly to the breaker within 3 blocks at 0.2 b/t for 20 ticks. */
public final class ItemMagnetRules {

    public static final double RANGE = 3.0;
    public static final double SPEED = 0.2;
    public static final int TICKS = 20;

    private ItemMagnetRules() {
    }

    /**
     * Velocity toward the breaker, given the vector from item to breaker, or {@code null} when out of range or
     * already there.
     */
    public static double[] pull(double dx, double dy, double dz, double maxRange, double speed) {
        double d2 = dx * dx + dy * dy + dz * dz;
        if (!(d2 <= maxRange * maxRange) || d2 < 1.0E-6) {
            return null;
        }
        double d = Math.sqrt(d2);
        return new double[] {dx / d * speed, dy / d * speed, dz / d * speed};
    }

    public static boolean active(boolean toolIronOrBetter, int itemAgeTicks) {
        return toolIronOrBetter && itemAgeTicks <= TICKS;
    }
}
