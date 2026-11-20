package za.co.neroland.nerospace.block.rules;

/**
 * Brine Salt Ore wet seam: mined with brine in any of the 6 neighbours the ore gives twice the salt, capped at
 * {@link #CAP} per stack.
 */
public final class WetSeamRules {

    /** Largest salt stack one break gives. */
    public static final int CAP = 8;

    private WetSeamRules() {
    }

    public static int multiplier(boolean brineAdjacent) {
        return brineAdjacent ? 2 : 1;
    }

    public static int count(int base, int multiplier) {
        return Math.min(CAP, Math.max(0, base) * Math.max(1, multiplier));
    }
}
