package za.co.neroland.nerospace.block.rules;

/** Mirror Crystal: lit (5) only while it can see the sky and stands in the Nyxara nightside biome. */
public final class StarlitRules {

    public static final int LIGHT = 5;

    private StarlitRules() {
    }

    public static boolean starlit(boolean seesSky, boolean nightsideBiome) {
        return seesSky && nightsideBiome;
    }

    public static int light(boolean starlit) {
        return starlit ? LIGHT : 0;
    }
}
