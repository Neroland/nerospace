package za.co.neroland.nerospace.block.rules;

/** Dry Ice (002 §1.2, §2.1.2): one-shot freeze of adjacent water sources; sublimates in light above 11. */
public final class DryIceRules {

    public static final int SUBLIMATE_LIGHT = 11;
    public static final int VANISH_ONE_IN = 12;
    public static final int CLOUD_PARTICLES = 4;

    private DryIceRules() {
    }

    /** Bit {@code i} set when neighbour {@code i} (0..5) is a water source to freeze. Never more than 6. */
    public static int freezeTargets(boolean[] adjacentIsSourceWater) {
        int mask = 0;
        for (int i = 0; i < Math.min(6, adjacentIsSourceWater.length); i++) {
            if (adjacentIsSourceWater[i]) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    public static boolean sublimates(int lightLevel) {
        return lightLevel > SUBLIMATE_LIGHT;
    }

    /** {@code roll} uniform in [0, {@link #VANISH_ONE_IN}). */
    public static boolean vanishes(int lightLevel, int roll) {
        return sublimates(lightLevel) && roll == 0;
    }
}
