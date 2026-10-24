package za.co.neroland.nerospace.client.sky;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;

import za.co.neroland.nerospace.client.ClientPlanetClock;
import za.co.neroland.nerospace.client.ClientPlanetWeather;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * The single, data-driven planet sky renderer (decision 003 §5–6, hybrid path). Each loader's sky hook asks
 * {@link #frame} for the current level and, when it returns a frame, hands drawing over to the renderer; when it
 * returns {@code null} the vanilla skybox renders untouched.
 *
 * <p><b>Stage 1 is plumbing only.</b> Every shipped profile is a placeholder with {@code vanillaSkybox: true}, so
 * {@link #frame} returns null for all planets and the three existing worlds look exactly as in 1.x (end sky on
 * Cindara/Glacira/station, overworld sky on Greenxertz). Stage 6 replaces the profiles and fills in the drawing.
 * Sky rendering is never a block-entity renderer and never reads block state.</p>
 */
public final class PlanetSkyRenderer {

    private PlanetSkyRenderer() {
    }

    /** Everything a loader hook needs to draw one sky frame: profile, clock phase and weather. */
    public record Frame(PlanetDefinition planet, SkyProfile profile, double phase, double daylight,
            WeatherEvent weather, float weatherIntensity) {
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
        return new Frame(planet, profile, phase, planet.daylight().at(phase), ClientPlanetWeather.event(level),
                ClientPlanetWeather.intensity(level));
    }

    /**
     * Whether a loader hook should take over the sky for this level this frame. Stage 1: always false (see the
     * class note); the hooks call it so Stage 6 only has to add drawing.
     */
    public static boolean overridesSky(@Nullable ClientLevel level) {
        return frame(level) != null;
    }

    /**
     * Draws one frame. @return true when the planet sky was drawn and the vanilla sky must be skipped.
     * Stage 1: draws nothing and returns false, so the vanilla sky always renders (no visual change).
     */
    public static boolean draw(@Nullable Frame frame) {
        return false;
    }

    /** Loader hooks call this: frame + draw for the current level. @return true to suppress the vanilla sky. */
    public static boolean renderCurrent(@Nullable ClientLevel level) {
        return draw(frame(level));
    }
}
