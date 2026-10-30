package za.co.neroland.nerospace.client.sky;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;

import za.co.neroland.nerospace.client.ClientPlanetClock;
import za.co.neroland.nerospace.client.ClientPlanetWeather;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.nyxara.worldgen.FixedXBands;

/**
 * The single, data-driven planet sky (hybrid path). Each loader's sky hook asks {@link #frame} for the current level
 * and, when it returns a frame, turns {@link #scene} into draw calls; when it returns {@code null} the vanilla skybox
 * renders untouched. A profile with {@code vanillaSkybox: true} (or no profile file) keeps the vanilla sky.
 *
 * <p>Drawing exists on NeoForge 26.3 (its {@code CustomSkyboxRenderer}, selected per dimension type by the
 * {@code neoforge:custom_skybox} attribute). Every other cell shows the dimension type's own colour attributes, which
 * carry each planet's palette. Sky rendering is never a block-entity renderer and never reads block state.</p>
 */
public final class PlanetSkyRenderer {

    private PlanetSkyRenderer() {
    }

    /** Everything a loader hook needs to draw one sky frame: profile, clock phase and weather. */
    public record Frame(PlanetDefinition planet, SkyProfile profile, double phase, double days, double daylight,
            WeatherEvent weather, float weatherIntensity, int bandWidth) {
    }

    /**
     * The frame to draw for {@code level}, or null to leave the vanilla sky alone (not a planet, no profile, or a
     * profile that asks for the vanilla skybox).
     */
    @Nullable
    public static Frame frame(@Nullable ClientLevel level) {
        if (level == null) {
            return null;
        }
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return null;
        }
        SkyProfile profile = SkyProfiles.get(planet.traits().skyProfile());
        if (profile.vanillaSkybox()) {
            return null;
        }
        double phase = ClientPlanetClock.phase(level);
        return new Frame(planet, profile, phase, ClientPlanetClock.days(level), planet.daylight().at(phase),
                ClientPlanetWeather.event(level), ClientPlanetWeather.intensity(level),
                ClientPlanetClock.bandWidth(level, NerospaceConfig.nyxaraTwilightBandWidth()));
    }

    /** Whether a loader hook should take over the sky for this level this frame. */
    public static boolean overridesSky(@Nullable ClientLevel level) {
        return frame(level) != null;
    }

    /**
     * What the frame shows: colours, stars, galaxy, aurora, haze and every body's direction.
     *
     * @param position  the camera's coordinate on the profile's {@code positionBlend} axis
     * @param timeTicks game time plus the partial tick
     */
    public static SkyScene scene(Frame frame, double position, double timeTicks) {
        int bandWidth = frame.profile().positionBlend()
                .map(b -> "nyxara.twilightBandWidth".equals(b.bandWidthConfig())
                        ? frame.bandWidth() : FixedXBands.DEFAULT_WIDTH)
                .orElse(FixedXBands.DEFAULT_WIDTH);
        return SkyScene.compute(frame.profile(), frame.phase(), frame.days(), frame.daylight(), frame.weather().name(),
                frame.weatherIntensity(), position, bandWidth, timeTicks);
    }

    /** The coordinate {@link #scene} wants for the profile's blend axis ({@code "z"} or, by default, x). */
    public static double blendPosition(Frame frame, double x, double z) {
        return frame.profile().positionBlend().map(b -> "z".equalsIgnoreCase(b.axis()) ? z : x).orElse(x);
    }
}
