package za.co.neroland.nerospace.block.rules;

/**
 * Oxidant Ice radiolysis: under open sky and a radiation level of 2 or more the ice gains one charge on a random
 * tick, up to 3. Mined, it gives one crystal plus one per charge. Fully charged ice stops ticking.
 */
public final class RadiolysisRules {

    public static final int MAX_CHARGE = 3;
    /** Lowest radiation level that charges the ice. */
    public static final int MIN_RADIATION = 2;

    private RadiolysisRules() {
    }

    public static boolean gains(boolean skyExposed, int radiationLevel, int charge) {
        return skyExposed && radiationLevel >= MIN_RADIATION && charge < MAX_CHARGE;
    }

    /** Crystals for a charge, clamped to 0..3: 1 + charge. */
    public static int yield(int charge) {
        return 1 + Math.max(0, Math.min(MAX_CHARGE, charge));
    }

    /** Whether a block at this charge still takes random ticks. */
    public static boolean ticks(int charge) {
        return charge < MAX_CHARGE;
    }

    /** The radiation level the ice sees: the biome base plus one while a flare runs. */
    public static int level(int baseLevel, boolean flareActive) {
        return baseLevel + (flareActive ? 1 : 0);
    }
}
