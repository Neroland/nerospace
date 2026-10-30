package za.co.neroland.nerospace.world.planet;

/**
 * Pure effective-radiation arithmetic (0..3). The ambient value is the planet trait, overridden by Nyxara's band
 * biomes (nightside 3, dayside 2, twilight band 1). Weather raises it for exposed positions outside a sealed oxygen
 * field: a Mars dust storm lifts it to 2, a Nyxara flare adds 1 on the dayside and in the band (the nightside faces
 * away from the sun). A suit's Rad Lining shields the resulting {@code RADIATION} hazard; it does not change the level.
 */
public final class RadiationRules {

    /** Effective radiation at or above this hurts an unshielded player. */
    public static final int DAMAGE_THRESHOLD = 2;
    public static final int MAX = 3;
    /** Level a Mars dust storm raises an exposed position to. */
    public static final int DUST_STORM_LEVEL = 2;

    public static final String NYXARA_NIGHTSIDE = "nerospace:nyxara_nightside";
    public static final String NYXARA_DAYSIDE = "nerospace:nyxara_dayside";
    public static final String NYXARA_BAND = "nerospace:nyxara_twilight_band";

    private RadiationRules() {
    }

    /** Ambient level: Nyxara band biomes override the trait value. */
    public static int base(int traitRadiation, String biomeId) {
        if (NYXARA_NIGHTSIDE.equals(biomeId)) {
            return 3;
        }
        if (NYXARA_DAYSIDE.equals(biomeId)) {
            return 2;
        }
        if (NYXARA_BAND.equals(biomeId) || isTerraformedGrove(biomeId)) {
            return 1; // the terraformed twilight grove only roots in the band, so it keeps the band's level
        }
        return clamp(traitRadiation);
    }

    /** The three terraformed twilight-grove biomes ({@code terraformed_nyxara_rooted/hydrated/living}). */
    public static boolean isTerraformedGrove(String biomeId) {
        return biomeId != null && biomeId.startsWith("nerospace:terraformed_nyxara_");
    }

    /** Whether a flare adds its +1 here: an active flare, an exposed and unsealed position, not the nightside. */
    public static boolean flareApplies(WeatherEvent event, String biomeId, boolean exposedUnsealed) {
        return event == WeatherEvent.FLARE && exposedUnsealed && !NYXARA_NIGHTSIDE.equals(biomeId);
    }

    /** Effective level from the ambient value and the weather at an exposed-unsealed (or sheltered) position. */
    public static int effective(int base, WeatherEvent event, String biomeId, boolean exposedUnsealed) {
        int level = clamp(base);
        if (event == WeatherEvent.DUST_STORM && exposedUnsealed) {
            level = Math.max(level, DUST_STORM_LEVEL);
        }
        if (flareApplies(event, biomeId, exposedUnsealed)) {
            level += 1;
        }
        return clamp(level);
    }

    /**
     * Damage per radiation hit for an unshielded, exposed player, graded by the effective level (0..3): nothing
     * below {@link #DAMAGE_THRESHOLD} (the twilight band, a quiet Mars), 1 at level 2 (Mars dust storm, Nyxara
     * dayside), 2 at level 3 (the nightside, or the dayside under a flare). The O₂ drain penalty applies on top.
     */
    public static float damageFor(int effectiveLevel) {
        int level = clamp(effectiveLevel);
        return level < DAMAGE_THRESHOLD ? 0.0F : (float) (level - DAMAGE_THRESHOLD + 1);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(MAX, v));
    }
}
