package za.co.neroland.nerospace.block.rules;

/** Truss run placement (002 §1.7): sneak-placing lays up to 8 in a line, 1 item each, stopping at the first non-air. */
public final class RunPlacementRules {

    public static final int MAX_RUN = 8;

    private RunPlacementRules() {
    }

    /** {@code pathIsAir[0]} is the clicked cell; the result is how many to place (0 if the first cell is blocked). */
    public static int runLength(int itemsAvailable, boolean[] pathIsAir, int max) {
        int limit = Math.min(Math.min(max, MAX_RUN), Math.max(0, itemsAvailable));
        int n = 0;
        while (n < limit && n < pathIsAir.length && pathIsAir[n]) {
            n++;
        }
        return n;
    }
}
