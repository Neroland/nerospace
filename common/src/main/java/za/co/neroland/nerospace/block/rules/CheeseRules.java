package za.co.neroland.nerospace.block.rules;

/**
 * Cheese (002 §5): the wheel is 9 cheese in, 9 out. Each slice raises {@code BITES} (0–8); the ninth slice removes
 * the block; breaking drops {@code 9 − BITES}.
 */
public final class CheeseRules {

    public static final int SLICES = 9;
    public static final int MAX_BITES = SLICES - 1;
    public static final int ORE_MIN = 1;
    public static final int ORE_MAX = 2;
    public static final int FORTUNE_BONUS_MAX = 2;
    public static final int NUTRITION = 4;
    public static final float SATURATION = 0.6F;
    public static final int REGEN_TICKS = 400;

    private CheeseRules() {
    }

    /** {@code BITES} after a slice, or −1 when this slice removes the block. */
    public static int afterSlice(int bites) {
        int b = Math.max(0, Math.min(MAX_BITES, bites));
        return b >= MAX_BITES ? -1 : b + 1;
    }

    public static int breakDrops(int bites) {
        return SLICES - Math.max(0, Math.min(MAX_BITES, bites));
    }

    /** Ore drops before fortune: clamped to 1–2; fortune adds up to +2. */
    public static int oreDrops(int base, int fortuneBonus) {
        return Math.max(ORE_MIN, Math.min(ORE_MAX, base)) + Math.max(0, Math.min(FORTUNE_BONUS_MAX, fortuneBonus));
    }
}
