package za.co.neroland.nerospace.block.rules;

/**
 * Red Regolith dune drift (002 §1.2, §3, §4): during a dust storm one surface block per chunk per
 * {@link #INTERVAL_TICKS} moves one block downwind, at most {@link #MAX_PER_STORM} per chunk per storm. A drift is
 * a move — the block count is conserved. Player-placed blocks never move.
 */
public final class DriftRules {

    public static final int INTERVAL_TICKS = 100;
    public static final int MOVES_PER_INTERVAL = 1;
    public static final int MAX_PER_STORM = 16;

    private DriftRules() {
    }

    public static boolean canDrift(boolean storm, boolean playerPlaced, boolean airAbove, boolean downwindAir,
            boolean downwindSupported, int movesThisStorm, int cap) {
        int effective = Caps.lower(MAX_PER_STORM, cap);
        return storm && !playerPlaced && airAbove && downwindAir && downwindSupported && movesThisStorm < effective;
    }

    /** The horizontal storm direction index (0..3, N/E/S/W order of the caller) fixed for a whole event. */
    public static int stormDirection(long eventSeed) {
        return (int) ((eventSeed >>> 7) & 3L);
    }
}
