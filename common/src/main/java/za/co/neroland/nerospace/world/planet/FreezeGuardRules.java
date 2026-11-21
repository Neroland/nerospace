package za.co.neroland.nerospace.world.planet;

/**
 * Pure guard for weather that freezes open water: only a biome whose base temperature is below 0.15 lets a plume
 * fall freeze it. Keeps a terraformed sea open under a plume fall.
 *
 * <p>Vanilla's own random-tick freeze ({@code Biome.shouldFreeze}) reads the <em>height-adjusted</em> temperature
 * instead, mirrored by {@link #vanillaTemperature}: above a snow line the temperature drops by
 * {@code (noise * 8 + y - snowLine) * 0.05 / 40}, noise in [-1, 1]. The snow line is the fixed Y 80 on 1.21.x and
 * {@code seaLevel + 17} on 26.x. Europa's terraformed sea sits at Y 176..212, so its biomes need a base of at least
 * 0.325 to stay open on every line (2.4.0 verifier fix V4-01: 0.35).</p>
 */
public final class FreezeGuardRules {

    /** Vanilla freezes water where the (height-adjusted) temperature is below this. */
    public static final float FREEZE_BELOW = 0.15F;
    /** The 1.21.x snow line (a fixed Y). */
    public static final int LEGACY_SNOW_LINE = 80;
    /** The 26.x snow line above the level's sea level. */
    public static final int SNOW_LINE_ABOVE_SEA = 17;
    /** Largest value of vanilla's temperature noise (scaled by 8 in the formula). */
    public static final float NOISE_MAX = 1.0F;

    private FreezeGuardRules() {
    }

    public static boolean biomeFreezes(float baseTemperature) {
        return baseTemperature < FREEZE_BELOW;
    }

    /**
     * Vanilla's height-adjusted temperature at {@code y} for a biome of {@code baseTemperature} (no temperature
     * modifier). {@code legacy} = the 1.21.x formula (snow line Y 80); otherwise the 26.x one (sea level + 17).
     * {@code noise} is the temperature noise value in [-1, 1].
     */
    public static float vanillaTemperature(float baseTemperature, int y, int seaLevel, boolean legacy, float noise) {
        int snowLine = legacy ? LEGACY_SNOW_LINE : seaLevel + SNOW_LINE_ABOVE_SEA;
        if (y <= snowLine) {
            return baseTemperature;
        }
        float v = noise * 8.0F;
        return baseTemperature - (v + y - snowLine) * 0.05F / 40.0F;
    }

    /** Whether vanilla's own freeze can ice water at {@code y} in the worst case (noise at its maximum). */
    public static boolean vanillaFreezes(float baseTemperature, int y, int seaLevel, boolean legacy) {
        return vanillaTemperature(baseTemperature, y, seaLevel, legacy, NOISE_MAX) < FREEZE_BELOW;
    }
}
