package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Decision 001 §18.1: on every planet a solar panel's output, averaged over a full planet day (and, on Mars,
 * over the default dust-storm duty cycle), is at least the 1.x flat 2.0. The station stays a constant 2.0;
 * {@code solar.clockDriven=false} restores the flat 2.0 everywhere.
 */
class SolarAverageTest {

    private static final int SAMPLES = 20_000;

    @Test
    @DisplayName("every planet's day-average is at least the 1.x flat 2.0")
    void averageAtLeastLegacy() {
        for (PlanetDefinition p : Planets.ALL) {
            double avg = SolarMath.dayAverage(p, 1.0, SAMPLES);
            assertTrue(avg >= SolarMath.LEGACY_FLAT, p + " averages " + avg);
        }
    }

    @Test
    @DisplayName("Mars: clear-sky average >= 2.0; worst-case storm-weighted average >= the 1.6 floor")
    void marsStormDutyCycle() {
        assertTrue(SolarMath.dayAverage(Planets.MARS, 1.0, SAMPLES) >= SolarMath.LEGACY_FLAT, "Mars clear-sky");
        WeatherParams w = Planets.MARS.weather();
        double meanDuration = (w.minDurationTicks() + w.maxDurationTicks()) / 2.0;
        double duty = meanDuration / w.meanIntervalTicks();
        double weatherFactor = 1.0 - 0.8 * duty; // worst case: every storm at intensity 1.0
        double avg = SolarMath.dayAverage(Planets.MARS, weatherFactor, SAMPLES);
        assertTrue(avg >= 1.6, "Mars storm-weighted average " + avg);
    }

    @Test
    @DisplayName("final solar constants (owner-ruled)")
    void constants() {
        assertEquals(2.0, Planets.STATION.planetSolar());
        assertEquals(4.0, Planets.LUNA.planetSolar());
        assertEquals(4.8, Planets.MARS.planetSolar());
        assertEquals(4.6, Planets.CINDARA.planetSolar());
        assertEquals(4.8, Planets.GLACIRA.planetSolar());
        assertEquals(3.8, Planets.GREENXERTZ.planetSolar());
        assertEquals(3.4, Planets.NYXARA.planetSolar());
    }

    @Test
    @DisplayName("the station is a constant 2.0; clockDriven=false is a flat 2.0 everywhere")
    void stationAndLegacy() {
        for (int i = 0; i < 10; i++) {
            assertEquals(2.0, SolarMath.factor(Planets.STATION, i / 10.0, 1.0, true), 1.0E-9);
        }
        for (PlanetDefinition p : Planets.ALL) {
            assertEquals(2.0, SolarMath.factor(p, 0.75, 0.0, false), 1.0E-9);
        }
    }
}
