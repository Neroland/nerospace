package za.co.neroland.nerospace.block.rules;

/**
 * Lineae Salt seep: a block with a cavity or brine within 12 cells below it shows a seep mist, which is how a player
 * reads what lies under a ridge. Client particles only; the column is scanned on one call in {@link #GATE}.
 */
public final class SeepRules {

    /** Deepest cell below the block that still counts. */
    public static final int MAX_DEPTH = 12;
    /** The column is scanned on one display call in this many. */
    public static final int GATE = 40;
    /** Particles shown by one scan that finds a cavity. */
    public static final int PARTICLES_PER_CALL = 1;

    private SeepRules() {
    }

    /** True for a cavity 1..12 cells below; 0 means none was found. */
    public static boolean seeps(int depthToCavity) {
        return depthToCavity >= 1 && depthToCavity <= MAX_DEPTH;
    }

    /** Whether this display call scans, from a roll in {@code [0, GATE)}. */
    public static boolean scans(int roll) {
        return roll == 0;
    }

    /**
     * Depth of the first open cell below the block (1 = directly below), or 0 when the first {@link #MAX_DEPTH} cells
     * are all closed. Cells past the limit are ignored.
     */
    public static int depthToCavity(boolean[] openBelow) {
        int n = Math.min(openBelow.length, MAX_DEPTH);
        for (int i = 0; i < n; i++) {
            if (openBelow[i]) {
                return i + 1;
            }
        }
        return 0;
    }
}
