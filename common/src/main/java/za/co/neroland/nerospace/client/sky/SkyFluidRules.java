package za.co.neroland.nerospace.client.sky;

/**
 * When the planet sky steps aside. With the camera inside water, lava or powder snow, vanilla keeps the sky behind a
 * short, thick fog; the planet sky's stars, bodies and galaxy ignore that fog, and so do vanilla's stars (the planets
 * use the overworld sky type with bright stars), so either would show through the water. In those cases no sky draws
 * at all on a planet and only the fog colour shows.
 *
 * <p>Takes the vanilla {@code FogType} constant's name so the rule stays pure and works on every Minecraft line.</p>
 */
public final class SkyFluidRules {

    private SkyFluidRules() {
    }

    /** Whether the camera's fog type hides the sky (so the planet sky must not draw). */
    public static boolean hidesPlanetSky(String fogTypeName) {
        if (fogTypeName == null) {
            return false;
        }
        return switch (fogTypeName) {
            case "WATER", "LAVA", "POWDER_SNOW" -> true;
            default -> false;
        };
    }
}
