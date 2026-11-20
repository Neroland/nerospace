package za.co.neroland.nerospace.block.rules;

/**
 * Cryovent tap: a charged vent used with an empty bucket gives exactly one bucket of brine and loses its charge; it is
 * charged again {@link #RECHARGE_TICKS} ticks later.
 */
public final class VentTapRules {

    /** Ticks from a tap to the next charge. */
    public static final int RECHARGE_TICKS = 600;
    /** Buckets one tap gives. */
    public static final int BUCKETS_PER_TAP = 1;

    private VentTapRules() {
    }

    public static boolean canTap(boolean charged, boolean holdingEmptyBucket) {
        return charged && holdingEmptyBucket;
    }

    /** The charge after a tap: always spent. */
    public static boolean chargedAfterTap() {
        return false;
    }

    /** Whether an uncharged vent recharges once its delay has passed. */
    public static boolean recharges(boolean charged) {
        return !charged;
    }
}
