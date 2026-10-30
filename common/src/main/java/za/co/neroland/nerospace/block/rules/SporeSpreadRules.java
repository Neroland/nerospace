package za.co.neroland.nerospace.block.rules;

/** Spore Moss spread: 1-in-8 per random tick onto adjacent Xertz Soil, ×3 in a bloom, cap 48 per chunk. */
public final class SporeSpreadRules {

    public static final int ONE_IN = 8;
    public static final int CAP = 48;
    public static final int BLOOM_BOOST = 3;
    public static final int LIGHT = 4;

    private SporeSpreadRules() {
    }

    public static boolean canSpread(boolean onSoil, int countInChunk, int cap, boolean bloom, int roll) {
        return SpreadRules.canSpread(onSoil, countInChunk, Caps.lower(CAP, cap), bloom ? BLOOM_BOOST : 1, roll);
    }
}
