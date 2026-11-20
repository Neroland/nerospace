package za.co.neroland.nerospace.world.planet;

/**
 * Pure guard for weather that freezes open water: only a biome cold enough to freeze water by vanilla rules lets it
 * (base temperature below 0.15). Keeps a terraformed sea open under a plume fall.
 */
public final class FreezeGuardRules {

    /** Vanilla freezes sky-exposed water in biomes whose base temperature is below this. */
    public static final float FREEZE_BELOW = 0.15F;

    private FreezeGuardRules() {
    }

    public static boolean biomeFreezes(float baseTemperature) {
        return baseTemperature < FREEZE_BELOW;
    }
}
