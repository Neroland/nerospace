package za.co.neroland.nerospace.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherState;

/** The client weather smoothing behind the sky's weather fades: no pops, and a minute of recovery after an event. */
class ClientPlanetWeatherSmoothingTest {

    @Test
    @DisplayName("a forced peak fades in over about a second and a half instead of popping")
    void fadesIn() {
        ClientPlanetWeather.reset("test", 0.0F);
        ClientPlanetWeather.step(1.0F, WeatherState.PEAK, WeatherEvent.DIAMOND_DUST);
        assertTrue(ClientPlanetWeather.smoothedAt(1.0F) < 0.15F, "first tick must not pop");
        assertTrue(ClientPlanetWeather.smoothedAt(0.5F) < ClientPlanetWeather.smoothedAt(1.0F), "interpolates");
        int ticks = 1;
        while (ClientPlanetWeather.smoothedAt(1.0F) < 0.9F) {
            ClientPlanetWeather.step(1.0F, WeatherState.PEAK, WeatherEvent.DIAMOND_DUST);
            ticks++;
        }
        assertTrue(ticks >= 20 && ticks <= 40, "reaches 90 % in " + ticks + " ticks");
    }

    @Test
    @DisplayName("a cleared event falls smoothly and leaves a one-minute recovery after-image")
    void clearsWithRecovery() {
        ClientPlanetWeather.reset("test", 1.0F);
        ClientPlanetWeather.step(1.0F, WeatherState.PEAK, WeatherEvent.DUST_STORM);
        assertEquals(0.0F, ClientPlanetWeather.recoveryForTest(), 1e-6F);
        // The server clears the storm in one packet (weather clear / gallery reset).
        ClientPlanetWeather.step(0.0F, WeatherState.CLEAR, WeatherEvent.NONE);
        assertTrue(ClientPlanetWeather.smoothedAt(1.0F) > 0.85F, "falls, never snaps");
        assertEquals(WeatherEvent.DUST_STORM, ClientPlanetWeather.recoveryEventForTest());
        assertEquals(1.0F, ClientPlanetWeather.recoveryForTest(), 1e-6F);
        for (int i = 0; i < 600; i++) {
            ClientPlanetWeather.step(0.0F, WeatherState.CLEAR, WeatherEvent.NONE);
        }
        assertEquals(0.5F, ClientPlanetWeather.recoveryForTest(), 0.01F);
        assertEquals(0.0F, ClientPlanetWeather.smoothedAt(1.0F), 1e-6F);
        for (int i = 0; i < 600; i++) {
            ClientPlanetWeather.step(0.0F, WeatherState.CLEAR, WeatherEvent.NONE);
        }
        assertEquals(WeatherEvent.NONE, ClientPlanetWeather.recoveryEventForTest());
        ClientPlanetWeather.reset(null, 0.0F);
    }
}
