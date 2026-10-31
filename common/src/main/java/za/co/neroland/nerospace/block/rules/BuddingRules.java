package za.co.neroland.nerospace.block.rules;

/**
 * Budding Nyx Crystal: each random tick it grows a cluster on one face 1 time in 5, 4 times in 5 while a flare runs.
 * Air becomes a stage-0 cluster facing out; an existing cluster on that face gains a stage, up to 3.
 */
public final class BuddingRules {

    public static final int ROLL = 5;
    public static final int MAX_STAGE = 3;

    private BuddingRules() {
    }

    /** {@code roll} is uniform in [0, 5). */
    public static boolean grows(int roll, boolean flare) {
        return roll >= 0 && roll < (flare ? 4 : 1);
    }

    /** The next stage for the face: -1 (empty) becomes 0, then +1 up to 3. */
    public static int next(int currentStageOrMinus1) {
        return Math.min(MAX_STAGE, Math.max(-1, currentStageOrMinus1) + 1);
    }
}
