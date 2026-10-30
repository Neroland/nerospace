package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.cindara.CindaraPlanet;

/** Planet clock arithmetic and per-planet daylight curves — pure, no server. */
class PlanetClockTest {

    private static final double EPS = 1.0E-9;

    @Test
    @DisplayName("phase advances by day length from the epoch and wraps")
    void phaseAdvances() {
        long day = 48_000L;
        assertEquals(0.0, ClockMath.phase(1_000L, 1_000L, 0L, day, 0.25, false, 0L), EPS);
        assertEquals(0.25, ClockMath.phase(1_000L + 12_000L, 1_000L, 0L, day, 0.25, false, 0L), EPS);
        assertEquals(0.5, ClockMath.phase(1_000L + day + 24_000L, 1_000L, 0L, day, 0.25, false, 0L), EPS);
    }

    @Test
    @DisplayName("a fixed clock always reads its pinned phase")
    void fixedClock() {
        assertEquals(0.30, ClockMath.phase(123_456L, 0L, 99L, 0L, 0.30, false, 0L), EPS);
        assertEquals(0.25, ClockMath.phase(0L, 0L, 0L, 0L, 1.25, false, 0L), EPS);
    }

    @Test
    @DisplayName("offsetFor makes the clock read the requested phase now")
    void setPhase() {
        long day = 24_600L;
        for (double target : new double[] {0.0, 0.1, 0.25, 0.6, 0.99}) {
            long offset = ClockMath.offsetFor(987_654L, 1_234L, day, target);
            double p = ClockMath.phase(987_654L, 1_234L, offset, day, 0.0, false, 0L);
            assertEquals(target, p, 1.0 / day);
        }
    }

    @Test
    @DisplayName("a frozen clock stops at the freeze time")
    void frozen() {
        long day = 72_000L;
        double atFreeze = ClockMath.phase(10_000L, 0L, 0L, day, 0.0, false, 0L);
        assertEquals(atFreeze, ClockMath.phase(50_000L, 0L, 0L, day, 0.0, true, 10_000L), EPS);
    }

    @Test
    @DisplayName("a LEVEL_TIME planet (Greenxertz) mirrors vanilla day time")
    void greenxertzMirrors() {
        assertEquals(ClockSource.LEVEL_TIME, Planets.GREENXERTZ.clockSource());
        assertEquals(24_000L, Planets.GREENXERTZ.traits().dayLengthTicks());
        assertEquals(0.25, ClockMath.mirrorPhase(6_000L), EPS);
        assertEquals(0.25, ClockMath.mirrorPhase(24_000L * 7 + 6_000L), EPS);
        for (PlanetDefinition p : Planets.ALL) {
            if (p != Planets.GREENXERTZ) {
                assertEquals(ClockSource.PLANET_CLOCK, p.clockSource(), p.id().asString());
            }
        }
    }

    @Test
    @DisplayName("day lengths match the design (Mars sol = 24 600 ticks; station and Nyxara fixed)")
    void dayLengths() {
        assertEquals(0L, Planets.STATION.traits().dayLengthTicks());
        assertEquals(672_000L, Planets.LUNA.traits().dayLengthTicks());
        assertEquals(24_600L, Planets.MARS.traits().dayLengthTicks());
        assertEquals(48_000L, Planets.CINDARA.traits().dayLengthTicks());
        assertEquals(72_000L, Planets.GLACIRA.traits().dayLengthTicks());
        assertEquals(0L, Planets.NYXARA.traits().dayLengthTicks());
        assertEquals(0.30, Planets.STATION.pinnedPhase(), EPS);
        assertEquals(0.25, Planets.NYXARA.pinnedPhase(), EPS);
    }

    @Test
    @DisplayName("Sol N counts from 1 and rolls over each day")
    void solCounter() {
        long sol = 24_600L;
        assertEquals(1L, ClockMath.dayNumber(500L, 500L, 0L, sol, false, 0L));
        assertEquals(1L, ClockMath.dayNumber(500L + sol - 1, 500L, 0L, sol, false, 0L));
        assertEquals(2L, ClockMath.dayNumber(500L + sol, 500L, 0L, sol, false, 0L));
        assertEquals(11L, ClockMath.dayNumber(500L + sol * 10 + 5, 500L, 0L, sol, false, 0L));
        assertEquals(1L, ClockMath.dayNumber(0L, 500L, 0L, sol, false, 0L), "never below 1");
        assertEquals(1L, ClockMath.dayNumber(9_999_999L, 0L, 0L, 0L, false, 0L), "fixed clock");
    }

    @Test
    @DisplayName("daylight: noon bright, midnight dark, Cindara eclipse near-total, Glacira twilight ramps")
    void curves() {
        for (PlanetDefinition p : Planets.ALL) {
            for (int i = 0; i < 100; i++) {
                double d = p.daylight().at(i / 100.0);
                assertTrue(d >= 0.0 && d <= 1.0, p + " out of range at " + i);
            }
        }
        assertEquals(1.0, Planets.LUNA.daylight().at(0.25), 1.0E-6);
        assertEquals(0.0, Planets.LUNA.daylight().at(0.70), 1.0E-6);
        assertEquals(0.05, Planets.LUNA.daylight().at(0.80), 1.0E-6, "earthshine");
        assertEquals(0.85, Planets.MARS.daylight().at(0.25), 1.0E-6);
        assertTrue(Planets.CINDARA.daylight().at(0.45) < 0.15, "eclipse totality in late afternoon");
        assertTrue(Planets.CINDARA.daylight().at(0.20) > 0.9, "Cindara midday");
        assertTrue(CindaraPlanet.inEclipse(0.45));
        assertTrue(!CindaraPlanet.inEclipse(0.60));
        assertEquals(0.75, Planets.GLACIRA.daylight().at(0.25), EPS, "Glacira noon on the shared convention");
        assertEquals(0.75, Planets.GLACIRA.daylight().at(0.06), EPS);
        assertEquals(0.375, Planets.GLACIRA.daylight().at(0.0), EPS, "sunrise mid-ramp");
        assertEquals(0.375, Planets.GLACIRA.daylight().at(0.5), EPS, "sunset mid-ramp");
        assertEquals(0.12, Planets.GLACIRA.daylight().at(0.75), EPS, "aurora floor at midnight");
        assertEquals(0.08, Planets.GREENXERTZ.daylight().at(0.75), EPS, "bio-glow floor");
        assertEquals(1.0, Planets.STATION.daylight().at(0.9), EPS);
        assertEquals(0.6, Planets.NYXARA.daylight().at(0.1), EPS);
    }

    @Test
    @DisplayName("a clock that was never ticked starts on its first read and then advances")
    void startsOnFirstRead() {
        PlanetClockState state = new PlanetClockState();
        assertTrue(!state.started());
        long day = 48_000L;
        double first = state.readPhase(10_000L, day, 0.25);
        assertTrue(state.started(), "first read starts the clock");
        assertEquals(0.0, first, EPS, "the clock starts at sunrise");
        assertEquals(0.25, state.readPhase(10_000L + 12_000L, day, 0.25), EPS, "it advances without any tick");
        assertEquals(2L, state.readDay(10_000L + day, day));
    }

    @Test
    @DisplayName("continuous days: fraction is the phase, no wrap at sunrise, frozen and fixed clocks hold")
    void continuousDays() {
        long day = 48_000L;
        for (long t = 1_000L; t < 1_000L + 5 * day; t += 997L) {
            double d = ClockMath.days(t, 1_000L, 0L, day, 0.25, false, 0L);
            assertEquals(ClockMath.phase(t, 1_000L, 0L, day, 0.25, false, 0L), d - Math.floor(d), 1e-9);
        }
        double before = ClockMath.days(1_000L + day - 1L, 1_000L, 0L, day, 0.25, false, 0L);
        double after = ClockMath.days(1_000L + day + 1L, 1_000L, 0L, day, 0.25, false, 0L);
        assertTrue(after > before && after - before < 1e-3, "no jump across sunrise");
        assertEquals(ClockMath.days(10_000L, 0L, 0L, day, 0.0, false, 0L),
                ClockMath.days(90_000L, 0L, 0L, day, 0.0, true, 10_000L), EPS);
        assertEquals(0.30, ClockMath.days(123_456L, 0L, 0L, 0L, 0.30, false, 0L), EPS);
        assertEquals(2.25, ClockMath.mirrorDays(54_000L), EPS);
    }
}
