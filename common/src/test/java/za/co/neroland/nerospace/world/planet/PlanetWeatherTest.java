package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The weather state machine — pure, no server. */
class PlanetWeatherTest {

    private static final WeatherParams STORM = new WeatherParams(WeatherEvent.DUST_STORM, 2_000L, 200, 400);

    @Test
    @DisplayName("CLEAR → RISING → PEAK → FADING → CLEAR, ramps at 0.02/s")
    void fullCycle() {
        WeatherMachine m = new WeatherMachine();
        Random rng = new Random(7);
        int steps = 0;
        while (m.state == WeatherState.CLEAR && steps++ < 100_000) {
            m.step(STORM, true, true, rng::nextDouble);
        }
        assertEquals(WeatherState.RISING, m.state);
        assertEquals(WeatherEvent.DUST_STORM, m.event);
        assertTrue(m.target >= WeatherMachine.MIN_TARGET && m.target <= WeatherMachine.MAX_TARGET);
        assertTrue(m.remainingTicks >= 200 && m.remainingTicks <= 400);
        float before = m.intensity;
        m.step(STORM, true, true, rng::nextDouble);
        assertEquals(before + WeatherMachine.RAMP_PER_SECOND, m.intensity, 1.0E-6F);
        steps = 0;
        while (m.state == WeatherState.RISING && steps++ < 1_000) {
            m.step(STORM, true, true, rng::nextDouble);
        }
        assertEquals(WeatherState.PEAK, m.state);
        assertEquals(m.target, m.intensity, 1.0E-6F);
        steps = 0;
        while (m.state == WeatherState.PEAK && steps++ < 1_000) {
            m.step(STORM, true, true, rng::nextDouble);
        }
        assertEquals(WeatherState.FADING, m.state);
        WeatherMachine.Change last = WeatherMachine.Change.NONE;
        steps = 0;
        while (m.state == WeatherState.FADING && steps++ < 1_000) {
            last = m.step(STORM, true, true, rng::nextDouble);
        }
        assertEquals(WeatherState.CLEAR, m.state);
        assertEquals(WeatherMachine.Change.ENDED, last);
        assertEquals(0.0F, m.intensity);
        assertTrue(m.cooldownTicks > 0, "cooldown restarts after an event");
    }

    @Test
    @DisplayName("nothing starts while conditions forbid it (eclipse / daylight)")
    void mayStartGate() {
        WeatherMachine m = new WeatherMachine();
        m.cooldownTicks = 0;
        for (int i = 0; i < 10_000; i++) {
            m.step(STORM, true, false, () -> 0.0);
        }
        assertEquals(WeatherState.CLEAR, m.state);
    }

    @Test
    @DisplayName("disabling weather forces an active event to CLEAR at once")
    void disabledForcesClear() {
        WeatherMachine m = new WeatherMachine();
        m.forceStart(STORM, 0.8F, 400, 1L);
        assertTrue(m.active());
        assertEquals(WeatherMachine.Change.ENDED, m.step(STORM, false, true, () -> 0.5));
        assertFalse(m.active());
        assertEquals(WeatherMachine.Change.NONE, m.step(STORM, false, true, () -> 0.5));
    }

    @Test
    @DisplayName("planets without weather never leave CLEAR")
    void noWeather() {
        WeatherMachine m = new WeatherMachine();
        for (int i = 0; i < 1_000; i++) {
            m.step(Planets.LUNA.weather(), true, true, () -> 0.0);
            m.step(Planets.STATION.weather(), true, true, () -> 0.0);
        }
        assertEquals(WeatherState.CLEAR, m.state);
    }

    @Test
    @DisplayName("each planet has the 003 event; config multipliers scale interval and duration")
    void tableAndScaling() {
        assertEquals(WeatherEvent.SPORE_BLOOM, Planets.GREENXERTZ.weather().event());
        assertEquals(WeatherEvent.DUST_STORM, Planets.MARS.weather().event());
        assertEquals(WeatherEvent.ASH_FALL, Planets.CINDARA.weather().event());
        assertEquals(WeatherEvent.DIAMOND_DUST, Planets.GLACIRA.weather().event());
        assertEquals(WeatherEvent.FLARE, Planets.NYXARA.weather().event());
        assertFalse(Planets.LUNA.weather().hasWeather());
        assertFalse(Planets.STATION.weather().hasWeather());
        WeatherParams scaled = STORM.scaled(2.0, 0.5);
        assertEquals(1_000L, scaled.meanIntervalTicks());
        assertEquals(100, scaled.minDurationTicks());
        assertEquals(200, scaled.maxDurationTicks());
    }

    @Test
    @DisplayName("Cindara ash fall never starts in the eclipse; Glacira diamond dust only in the dark")
    void startConditions() {
        assertFalse(Planets.CINDARA.weatherMayStart(0.45));
        assertFalse(Planets.CINDARA.weatherMayStart(0.38));
        assertFalse(Planets.CINDARA.weatherMayStart(0.52));
        assertTrue(Planets.CINDARA.weatherMayStart(0.30));
        assertTrue(Planets.CINDARA.weatherMayStart(0.60));
        assertFalse(Planets.GLACIRA.weatherMayStart(0.25));
        assertTrue(Planets.GLACIRA.weatherMayStart(0.75));
    }

    @Test
    @DisplayName("persisted enums are read by name; unknown names degrade safely")
    void byName() {
        assertEquals(WeatherEvent.FLARE, WeatherEvent.byName("FLARE"));
        assertEquals(WeatherEvent.NONE, WeatherEvent.byName("TORNADO"));
        assertEquals(WeatherState.PEAK, WeatherState.byName("PEAK"));
        assertEquals(WeatherState.CLEAR, WeatherState.byName(null));
    }
}
