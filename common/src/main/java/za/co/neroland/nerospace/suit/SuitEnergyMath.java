package za.co.neroland.nerospace.suit;

/** Battery size, module costs and the fail-safe reserve. Costs are FE per tick unless noted. */
public final class SuitEnergyMath {

    public static final int HOVER_PER_TICK = 40;
    public static final int CRUISE_PER_TICK = 60;
    public static final int BOOST_PER_TICK = 120;
    public static final int NIGHT_VISOR_PER_TICK = 1;
    public static final int STRIDE_PER_TICK = 4;
    public static final int LEAP_PER_JUMP = 200;
    public static final int SHOCK_PER_HEART = 50;
    public static final int SHIELD_PER_POINT = 400;
    public static final int SCANNER_PER_PULSE = 500;
    public static final double GRAVITY_FLOOR = 0.25D;

    private SuitEnergyMath() {
    }

    public static int capacity(SuitFrame frame, int powerCells, double capacityMultiplier) {
        double cells = 1.0D + 0.5D * Math.min(2, Math.max(0, powerCells));
        return Math.max(1, (int) Math.round(frame.battery * cells * capacityMultiplier));
    }

    public static int reserve(int capacity, int reservePercent) {
        int percent = Math.max(0, Math.min(20, reservePercent));
        return (capacity * percent) / 100;
    }

    public static int cost(int base, double useMultiplier) {
        if (useMultiplier <= 0.0D || base <= 0) {
            return 0;
        }
        return Math.max(0, (int) Math.round(base * useMultiplier));
    }

    /** Flight and hover cost, scaled by gravity and floored so the Moon is not free. */
    public static int flightCost(int basePerTick, double gravity, double useMultiplier) {
        double g = Math.max(GRAVITY_FLOOR, gravity);
        return cost((int) Math.round(basePerTick * g), useMultiplier);
    }

    public static boolean aboveReserve(int energy, int capacity, int reservePercent) {
        return energy > reserve(capacity, reservePercent);
    }
}
