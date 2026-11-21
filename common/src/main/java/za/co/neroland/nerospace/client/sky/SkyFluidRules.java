package za.co.neroland.nerospace.client.sky;

/**
 * When the planet sky steps aside. With the camera inside water, lava or powder snow, vanilla keeps the sky behind a
 * short, thick fog; the planet sky's stars, bodies and galaxy ignore that fog, so they would show through the water
 * and through the terrain the fog hides. In those cases the planet sky draws nothing and vanilla's own sky renders,
 * which the fog then covers as players expect.
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
