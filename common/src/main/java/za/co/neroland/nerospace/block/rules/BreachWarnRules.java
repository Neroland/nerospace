package za.co.neroland.nerospace.block.rules;

/**
 * Shell Ice breach warning: mining a block with water or brine within 6 cells along any of the six axes plays a
 * warning crack, pitched higher the closer the liquid is. Sound and a few particles only; nothing floods.
 */
public final class BreachWarnRules {

    /** Cells checked along each axis. */
    public static final int RANGE = 6;
    /** Particles shown with the crack. */
    public static final int PARTICLES = 4;
    private static final float BASE_PITCH = 0.6F;
    private static final float PITCH_STEP = 0.15F;

    private BreachWarnRules() {
    }

    /** True for a liquid 1..6 cells away; 0 means none was found. */
    public static boolean warns(int distanceToLiquid) {
        return distanceToLiquid >= 1 && distanceToLiquid <= RANGE;
    }

    /** Pitch of the crack: 0.6 at the far end of the range, rising 0.15 per cell closer. */
    public static float pitch(int distanceToLiquid) {
        int d = Math.max(1, Math.min(RANGE, distanceToLiquid));
        return BASE_PITCH + (RANGE - d) * PITCH_STEP;
    }

    /** The nearest of the per-axis distances (0 = none on that axis), or 0 when no axis found a liquid. */
    public static int nearest(int[] axisDistances) {
        int best = 0;
        for (int d : axisDistances) {
            if (d >= 1 && (best == 0 || d < best)) {
                best = d;
            }
        }
        return best;
    }
}
