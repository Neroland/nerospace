package za.co.neroland.nerospace.suit;

/**
 * Oxygen capacity and drain for a worn suit. Life support never costs energy. The numbers match today's
 * bare-lungs and plain-suit rates when no tank or reclaimer is installed.
 */
public final class SuitOxygenMath {

    public static final int BARE = 300;
    public static final int SUIT = 900;
    public static final int TANK = 450;
    public static final int SUIT_DRAIN = 3;
    public static final int BARE_DRAIN = 30;

    private SuitOxygenMath() {
    }

    public static int capacity(boolean fullSuit, int tanks, double capacityMultiplier) {
        int base = fullSuit ? SUIT + TANK * Math.min(2, Math.max(0, tanks)) : BARE;
        return scale(base, capacityMultiplier);
    }

    /**
     * Drain for one oxygen check. Reclaimers multiply the suited base by 0.67 each, never below half, before
     * the config multiplier. An unshielded hazard multiplies the result by 4, as today.
     */
    public static int drain(boolean suited, int reclaimers, boolean unshieldedHazard, double drainMultiplier) {
        double factor = 1.0D;
        if (suited) {
            int n = Math.min(2, Math.max(0, reclaimers));
            for (int i = 0; i < n; i++) {
                factor *= 0.67D;
            }
            factor = Math.max(0.5D, factor);
        }
        int base = (int) Math.round((suited ? SUIT_DRAIN : BARE_DRAIN) * factor);
        int drain = scale(base, drainMultiplier);
        return unshieldedHazard ? drain * 4 : drain;
    }

    /** Same clamp as {@code NerospaceConfig.scale}: at least 1. */
    public static int scale(int base, double multiplier) {
        return Math.max(1, (int) Math.round(base * multiplier));
    }
}
