package za.co.neroland.nerospace.block.rules;

/** Shadow Lichen: spreads 1-in-10 in light ≤ 3, cap 32 faces per chunk; withers 1-in-2 during a Flare. */
public final class LichenRules {

    public static final int MAX_LIGHT = 3;
    public static final int CAP = 32;
    public static final int ONE_IN = 10;
    public static final int WITHER_ONE_IN = 2;

    private LichenRules() {
    }

    /** Growth gate (the 1-in-10 roll is the caller's {@link SpreadRules} roll). */
    public static boolean canGrow(int light, int countInChunk, int cap, boolean flare) {
        return !flare && light <= MAX_LIGHT && countInChunk < Caps.lower(CAP, cap);
    }

    public static boolean canSpread(int light, int countInChunk, int cap, boolean flare, int roll) {
        return canGrow(light, countInChunk, cap, flare)
                && SpreadRules.canSpread(true, countInChunk, Caps.lower(CAP, cap), 1, roll);
    }

    /** {@code roll} uniform in [0, 2). */
    public static boolean withers(boolean flare, int roll) {
        return flare && roll == 0;
    }
}
