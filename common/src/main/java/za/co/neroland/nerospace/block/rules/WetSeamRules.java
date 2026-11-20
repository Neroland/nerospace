package za.co.neroland.nerospace.block.rules;

/**
 * Brine Salt Ore wet seam: mined with brine in any of the 6 neighbours the ore gives twice the salt, capped at
 * {@link #CAP} per stack. The cap never takes a wet break below the dry yield (a Fortune stack above the cap keeps
 * its size).
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
        int dry = Math.max(0, base);
        return Math.max(dry, Math.min(CAP, dry * Math.max(1, multiplier)));
    }
}
