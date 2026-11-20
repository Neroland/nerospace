package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.europa.EuropaPlanet;

/** Two weather events on one machine: start order, ownership, expected rates, and no change for one-event planets. */
class WeatherPickRulesTest {

    private static final WeatherParams P = new WeatherParams(WeatherEvent.DIAMOND_DUST, 60_000L, 3_000, 9_000);
    private static final WeatherParams S = new WeatherParams(WeatherEvent.FLARE, 68_160L, 1_800, 3_600);

    @Test
    @DisplayName("start order: secondary first, then primary; no secondary is the primary only")
    void noSecondaryIsPrimaryOnly() {
        assertEquals(List.of(P), WeatherPickRules.startOrder(P, WeatherParams.NONE));
        assertEquals(List.of(S, P), WeatherPickRules.startOrder(P, S));
        assertEquals(List.of(), WeatherPickRules.startOrder(WeatherParams.NONE, WeatherParams.NONE));
        for (PlanetDefinition planet : Planets.ALL) {
            if (planet != Planets.EUROPA) {
                assertFalse(planet.secondaryWeather().hasWeather(), planet.langId());
            }
        }
    }

    @Test
    @DisplayName("owner: the secondary owns its own event, the primary everything else")
    void owner() {
        assertEquals(S, WeatherPickRules.owner(P, S, WeatherEvent.FLARE));
        assertEquals(P, WeatherPickRules.owner(P, S, WeatherEvent.DIAMOND_DUST));
        assertEquals(P, WeatherPickRules.owner(P, S, WeatherEvent.NONE));
        assertEquals(P, WeatherPickRules.owner(P, WeatherParams.NONE, WeatherEvent.FLARE));
        assertEquals(P, WeatherPickRules.owner(P, S, null));
    }

    @Test
    @DisplayName("expected starts per Europa day: 0.5 radiation storms, 2.84 plume falls")
    void rates() {
        long day = Planets.EUROPA.traits().dayLengthTicks();
        assertEquals(85_200L, day);
        double tail = EuropaPlanet.TAIL_END - EuropaPlanet.TAIL_START;
        assertEquals(0.2, tail, 1.0E-9);
        assertEquals(0.5, WeatherPickRules.expectedStartsPerDay(Planets.EUROPA.secondaryWeather(), tail, day), 0.01);
        assertEquals(2.84, WeatherPickRules.expectedStartsPerDay(Planets.EUROPA.weather(), 1.0, day), 0.01);
        assertEquals(0.0, WeatherPickRules.expectedStartsPerDay(WeatherParams.NONE, 1.0, day));
    }

    @Test
    @DisplayName("the secondary rolls only on a clear machine whose cooldown has run out")
    void rollsSecondary() {
        assertTrue(WeatherPickRules.rollsSecondary(S, false, 0));
        assertFalse(WeatherPickRules.rollsSecondary(S, true, 0));
        assertFalse(WeatherPickRules.rollsSecondary(S, false, 20));
        assertFalse(WeatherPickRules.rollsSecondary(S, false, -1), "the first step sets the cooldown");
        assertFalse(WeatherPickRules.rollsSecondary(WeatherParams.NONE, false, 0));
    }

    private static List<String> trace(WeatherMachine m) {
        return List.of(m.state.name(), m.event.name(), Float.toString(m.intensity), Float.toString(m.target),
                Integer.toString(m.remainingTicks), Integer.toString(m.cooldownTicks), Long.toString(m.seed));
    }

    @Test
    @DisplayName("a planet without a secondary steps exactly as before: same states, same random draws (Mars)")
    void marsUnchanged() {
        WeatherParams mars = Planets.MARS.weather();
        WeatherMachine old = new WeatherMachine();
        WeatherMachine now = new WeatherMachine();
        Random a = new Random(2031L);
        Random b = new Random(2031L);
        List<List<String>> oldTrace = new ArrayList<>();
        List<List<String>> newTrace = new ArrayList<>();
        for (int i = 0; i < 40_000; i++) {
            boolean enabled = i % 9_000 < 8_500;
            double phase = (i % 1_230) / 1_230.0;
            old.step(mars, enabled, Planets.MARS.weatherMayStart(phase), a::nextDouble);
            WeatherPickRules.step(now, mars, Planets.MARS.secondaryWeather(), enabled,
                    e -> Planets.MARS.weatherMayStart(phase), b::nextDouble);
            oldTrace.add(trace(old));
            newTrace.add(trace(now));
        }
        assertEquals(oldTrace, newTrace);
        assertEquals(a.nextLong(), b.nextLong(), "both sides drew the same number of values");
        assertTrue(oldTrace.stream().anyMatch(t -> t.get(0).equals("PEAK")), "the run includes storms");
    }

    @Test
    @DisplayName("Europa: over 50 seeded days both events occur, and a storm only ever starts in the tail window")
    void europaBothEvents() {
        PlanetDefinition europa = Planets.EUROPA;
        long day = europa.traits().dayLengthTicks();
        WeatherMachine m = new WeatherMachine();
        Random rng = new Random(85_200L);
        int plumes = 0;
        int storms = 0;
        for (long tick = 0; tick < 50 * day; tick += WeatherMachine.STEP_TICKS) {
            double phase = (tick % day) / (double) day;
            WeatherMachine.Change c = WeatherPickRules.step(m, europa.weather(), europa.secondaryWeather(), true,
                    e -> europa.weatherMayStart(e, phase), rng::nextDouble);
            if (c == WeatherMachine.Change.STARTED) {
                if (m.event == WeatherEvent.FLARE) {
                    storms++;
                    assertTrue(EuropaPlanet.inTail(phase), "storm started at phase " + phase);
                    assertTrue(m.remainingTicks >= 1_800 && m.remainingTicks <= 3_600);
                } else {
                    assertEquals(WeatherEvent.DIAMOND_DUST, m.event);
                    plumes++;
                    assertTrue(m.remainingTicks >= 3_000 && m.remainingTicks <= 9_000);
                }
            }
        }
        assertTrue(plumes >= 1, "plume falls: " + plumes);
        assertTrue(storms >= 1, "radiation storms: " + storms);
        assertTrue(plumes > storms, plumes + " plume falls, " + storms + " storms");
    }
}
