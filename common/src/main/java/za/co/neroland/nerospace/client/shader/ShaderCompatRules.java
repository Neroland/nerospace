package za.co.neroland.nerospace.client.shader;

import java.util.Locale;
import java.util.Optional;

/**
 * Pure decision table for shader-pack dimension compatibility: given the player's setting, the world's sky
 * profile and what the active pack declares, which vanilla program set (if any) to report to the pack.
 * No shader-mod classes are touched here, so this is testable without one on the classpath.
 */
public final class ShaderCompatRules {

    /** {@code shaders.dimensionCompat}: {@code auto} applies the table, {@code off} never overrides. */
    public enum Mode {
        AUTO, OFF;

        /** Lenient: anything but {@code off} is {@link #AUTO} (the default). */
        public static Mode byName(String name) {
            return name != null && "off".equals(name.trim().toLowerCase(Locale.ROOT)) ? OFF : AUTO;
        }
    }

    private ShaderCompatRules() {
    }

    /**
     * The program set to report instead of the pack's default, or empty to leave the shader mod's own answer
     * alone.
     *
     * @param mode             the player's {@code shaders.dimensionCompat} setting
     * @param planetDimension  whether the current level is a Nerospace planet with a sky profile
     * @param profileChoice    the profile's {@code shaderDimension}
     * @param packNamesIt      whether the active pack names this dimension in its own per-dimension config
     */
    public static Optional<ShaderDimension> override(Mode mode, boolean planetDimension,
            ShaderDimension profileChoice, boolean packNamesIt) {
        if (mode != Mode.AUTO || !planetDimension || profileChoice == null) {
            return Optional.empty();
        }
        // The pack wins when it knows us.
        if (packNamesIt) {
            return Optional.empty();
        }
        // Overworld is what a pack does for an unknown dimension anyway: nothing to override.
        if (profileChoice == ShaderDimension.OVERWORLD) {
            return Optional.empty();
        }
        return Optional.of(profileChoice);
    }

    /**
     * Whether the planet sky is drawn at all this frame: always without a pack; with one, only while
     * {@code shaders.drawPlanetSky} is on (the pack's own sky shows otherwise).
     */
    public static boolean drawPlanetSky(boolean packActive, boolean drawPlanetSkySetting) {
        return !packActive || drawPlanetSkySetting;
    }
}
