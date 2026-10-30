package za.co.neroland.nerospace.block.rules;

/** Perchlorate oxidiser in the Fuel Refinery: +15 % output, one oxidiser per 4 buckets. */
public final class OxidiserRules {

    public static final double MULTIPLIER = 1.15;
    public static final int BUCKETS_PER_OXIDISER = 4;

    private OxidiserRules() {
    }

    public static double yieldMultiplier(boolean oxidiserPresent) {
        return oxidiserPresent ? MULTIPLIER : 1.0;
    }
}
