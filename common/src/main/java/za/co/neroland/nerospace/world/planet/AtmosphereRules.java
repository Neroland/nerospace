package za.co.neroland.nerospace.world.planet;

/**
 * Pure rule for whether terraformed ground is breathable. Everywhere it is, except on a planet whose terraform
 * table carries {@link TerraformTable.Rule#NO_ATMOSPHERE}: there the terraform branches are skipped and only oxygen
 * fields, pads and outside pressure count.
 */
public final class AtmosphereRules {

    private AtmosphereRules() {
    }

    /**
     * @param hasNoAtmosphere the level's planet table has {@code NO_ATMOSPHERE}
     * @param terraformed     the terraformed-chunk flag, or a terraform overlay at stage 1 or later
     */
    public static boolean terraformBreathable(boolean hasNoAtmosphere, boolean terraformed) {
        return !hasNoAtmosphere && terraformed;
    }
}
