package za.co.neroland.nerospace.block.rules;

/** Xertz Vine: grows down 1-in-10 per random tick to length 12, only into air. */
public final class VineRules {

    public static final int MAX_LENGTH = 12;
    public static final int ONE_IN = 10;

    private VineRules() {
    }

    public static boolean canGrowDown(int length, int max, boolean belowIsAir) {
        return belowIsAir && length < Math.min(max, MAX_LENGTH);
    }
}
