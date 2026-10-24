package za.co.neroland.nerospace.block.rules;

/** Regolith footprints (002 §1.1): a persistent {@code FOOTPRINT} state set by living walkers. */
public final class FootprintRules {

    /** Minimum fall this tick that counts as a step down onto the block. */
    public static final double MIN_FALL = 0.1;
    /** Minimum horizontal movement this tick that counts as walking. */
    public static final double MIN_MOVE = 0.2;
    /** Fall distance at or above which the client shows the landing dust puff. */
    public static final double PUFF_FALL = 1.0;
    /** Block particles in one landing puff. */
    public static final int PUFF_PARTICLES = 6;

    private FootprintRules() {
    }

    /** Whether a step imprints the block. Items, projectiles and spectators never do; already-imprinted short-circuits. */
    public static boolean shouldImprint(boolean livingEntity, boolean spectator, double fallDistance, double moveDelta,
            boolean alreadyImprinted) {
        if (!livingEntity || spectator || alreadyImprinted) {
            return false;
        }
        return fallDistance >= MIN_FALL || moveDelta >= MIN_MOVE;
    }

    /** Whether the footprint is cleared: a neighbour update from above, or a piston move. */
    public static boolean shouldClear(boolean fromAbove, boolean pistonMoved) {
        return fromAbove || pistonMoved;
    }

    /** As {@link #shouldClear(boolean, boolean)}, plus a re-step from the facing opposite to the imprint. */
    public static boolean shouldClear(boolean fromAbove, boolean pistonMoved, boolean steppedFromOpposite) {
        return shouldClear(fromAbove, pistonMoved) || steppedFromOpposite;
    }

    /** Whether a landing shows the dust puff (client only). */
    public static boolean showPuff(double fallDistance) {
        return fallDistance >= PUFF_FALL;
    }
}
