package za.co.neroland.nerospace.client;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.network.PlanetWeatherPayload;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherState;

/**
 * Client-side copy of the current level's weather, filled by {@link PlanetWeatherPayload}. Pure data holder
 * (no client-only imports). Render-only — the server decides everything.
 */
public final class ClientPlanetWeather {

    @Nullable
    private static volatile PlanetWeatherPayload current;

    private ClientPlanetWeather() {
    }

    public static void accept(PlanetWeatherPayload payload) {
        current = Float.isFinite(payload.intensity()) ? payload : null;
    }

    private static boolean matches(@Nullable PlanetWeatherPayload c, @Nullable Level level) {
        return c != null && level != null && c.dimension().equals(level.dimension().identifier());
    }

    public static WeatherEvent event(@Nullable Level level) {
        PlanetWeatherPayload c = current;
        if (!matches(c, level)) {
            return WeatherEvent.NONE;
        }
        WeatherEvent[] all = WeatherEvent.values();
        int i = c.event();
        return i >= 0 && i < all.length ? all[i] : WeatherEvent.NONE;
    }

    public static WeatherState state(@Nullable Level level) {
        PlanetWeatherPayload c = current;
        if (!matches(c, level)) {
            return WeatherState.CLEAR;
        }
        WeatherState[] all = WeatherState.values();
        int i = c.state();
        return i >= 0 && i < all.length ? all[i] : WeatherState.CLEAR;
    }

    public static float intensity(@Nullable Level level) {
        PlanetWeatherPayload c = current;
        return matches(c, level) ? Math.max(0.0F, Math.min(1.0F, c.intensity())) : 0.0F;
    }
}
