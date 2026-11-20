package za.co.neroland.nerospace.client.shader;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import za.co.neroland.nerospace.client.sky.SkyProfile;
import za.co.neroland.nerospace.client.sky.SkyProfiles;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * Client glue between the shader-mod hook and the pure {@link ShaderCompatRules}: reads the current level, its
 * planet's sky profile and the two client settings, and answers the hook. Settings are read on every call, so
 * a change applies without a restart (the shader mod re-resolves the dimension every frame).
 */
public final class ShaderCompat {

    private ShaderCompat() {
    }

    /**
     * Called from the shader mod's dimension lookup with its own answer. Returns the shader mod's constant
     * for the program set this world asks for, or null to keep {@code current}. Never throws.
     */
    public static Object remap(Object current) {
        try {
            Optional<ShaderDimension> override = override();
            return override.isEmpty() ? null : IrisBridge.dimensionConstant(override.get());
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** The program set the current world asks a pack for, or empty to leave the pack's own choice. */
    public static Optional<ShaderDimension> override() {
        ShaderCompatRules.Mode mode = ShaderCompatRules.Mode.byName(NerospaceConfig.shadersDimensionCompat());
        if (mode != ShaderCompatRules.Mode.AUTO) {
            return Optional.empty();
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return Optional.empty();
        }
        Optional<PlanetDefinition> planet = Planets.byDimension(level.dimension());
        if (planet.isEmpty()) {
            return Optional.empty();
        }
        SkyProfile profile = SkyProfiles.get(planet.get().traits().skyProfile());
        ShaderDimension choice = ShaderDimension.byName(profile.shaderDimension());
        if (choice == ShaderDimension.OVERWORLD) {
            return Optional.empty();
        }
        var id = level.dimension().identifier();
        boolean packNamesIt = IrisBridge.packNames(id.getNamespace(), id.getPath());
        return ShaderCompatRules.override(mode, true, choice, packNamesIt);
    }

    /**
     * Whether the planet sky should be drawn this frame: always without a shader pack; with one, only while
     * {@code shaders.drawPlanetSky} is on.
     */
    public static boolean drawPlanetSky() {
        return ShaderCompatRules.drawPlanetSky(IrisBridge.packActive(), NerospaceConfig.shadersDrawPlanetSky());
    }
}
