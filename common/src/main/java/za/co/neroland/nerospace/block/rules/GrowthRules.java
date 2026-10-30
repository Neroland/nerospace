package za.co.neroland.nerospace.block.rules;

/** Glowcap Fungus: bone meal grows a Giant Glowcap 40 % of the time if 5 air above and soil below. */
public final class GrowthRules {

    public static final int AIR_NEEDED = 5;
    public static final float BONEMEAL_CHANCE = 0.4F;

    private GrowthRules() {
    }

    public static boolean giantFits(int airAbove, boolean soilBelow) {
        return soilBelow && airAbove >= AIR_NEEDED;
    }
}
