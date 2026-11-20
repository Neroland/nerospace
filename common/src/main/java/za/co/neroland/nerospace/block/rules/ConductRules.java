package za.co.neroland.nerospace.block.rules;

/**
 * Vent Basalt conduction: basalt touching an active vent is at distance 1; otherwise it is one more than its warmest
 * basalt neighbour, capped at {@link #COLD}. Basalt at distance 3 or less carries the vent's warmth.
 */
public final class ConductRules {

    /** The cold state, and the value every non-basalt neighbour counts as. */
    public static final int COLD = 4;
    /** Largest distance that is still warm. */
    public static final int WARM_MAX = 3;

    private ConductRules() {
    }

    /** New distance from the six neighbours' distances ({@link #COLD} for anything that is not vent basalt). */
    public static int distance(int[] neighbourDistances, boolean touchingActiveVent) {
        if (touchingActiveVent) {
            return 1;
        }
        int min = COLD;
        for (int d : neighbourDistances) {
            if (d < min) {
                min = d;
            }
        }
        return Math.min(COLD, Math.max(1, min + 1));
    }

    public static boolean warm(int distance) {
        return distance <= WARM_MAX;
    }
}
