package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.PlanetSleepRules.Decision;
import za.co.neroland.nerospace.world.planet.PlanetSleepRules.Side;
import za.co.neroland.nerospace.world.planet.PlanetSleepRules.Window;
import za.co.neroland.nerospace.world.planet.PlanetSleepRules.World;

/** Sleeping on planets: the night window, the per-world decisions and vanilla's sleepers maths. Pure, no server. */
class PlanetSleepRulesTest {

    private static final Window W = PlanetSleepRules.DEFAULT_WINDOW;

    private static Decision clocked(double phase) {
        return PlanetSleepRules.decide(World.CLOCKED, true, false, phase, W, null);
    }

    @Test
    @DisplayName("the night window is [0.52, 0.98): edges, noon, sunset, midnight, dawn")
    void windowEdges() {
        assertEquals(Decision.REFUSE_DAY, clocked(0.519));
        assertEquals(Decision.SLEEP_SKIP, clocked(0.52));
        assertEquals(Decision.SLEEP_SKIP, clocked(0.75));
        assertEquals(Decision.SLEEP_SKIP, clocked(0.979));
        assertEquals(Decision.REFUSE_DAY, clocked(0.98));
        assertEquals(Decision.REFUSE_DAY, clocked(0.0));
        assertEquals(Decision.REFUSE_DAY, clocked(0.25));
        assertEquals(Decision.REFUSE_DAY, clocked(0.5));
        assertEquals(Decision.REFUSE_DAY, clocked(0.45), "an eclipse at 0.40-0.50 never opens the window");
        assertEquals(Decision.SLEEP_SKIP, clocked(1.75), "phases wrap");
    }

    @Test
    @DisplayName("a wrapping window crosses sunrise")
    void wrappingWindow() {
        Window w = new Window(0.9, 0.1);
        assertTrue(w.contains(0.95));
        assertTrue(w.contains(0.05));
        assertFalse(w.contains(0.5));
        assertFalse(w.contains(0.1));
    }

    @Test
    @DisplayName("fixed clocks: the station never sleeps; Nyxara rests on the Nightside only")
    void fixedClocks() {
        for (double phase : new double[] {0.25, 0.30, 0.75}) {
            assertEquals(Decision.REFUSE_STATION, PlanetSleepRules.decide(World.STATION, true, false, phase, W, null));
            assertEquals(Decision.SLEEP_REST,
                    PlanetSleepRules.decide(World.NYXARA, true, false, phase, W, Side.NIGHTSIDE));
            assertEquals(Decision.REFUSE_NYXARA_BRIGHT,
                    PlanetSleepRules.decide(World.NYXARA, true, false, phase, W, Side.BAND));
            assertEquals(Decision.REFUSE_NYXARA_BRIGHT,
                    PlanetSleepRules.decide(World.NYXARA, true, false, phase, W, Side.DAYSIDE));
        }
    }

    @Test
    @DisplayName("a frozen clock allows rest in the window without a skip; out of the window it still refuses")
    void frozenClock() {
        assertEquals(Decision.SLEEP_REST, PlanetSleepRules.decide(World.CLOCKED, true, true, 0.75, W, null));
        assertEquals(Decision.REFUSE_DAY, PlanetSleepRules.decide(World.CLOCKED, true, true, 0.25, W, null));
    }

    @Test
    @DisplayName("clock.enabled=false: every clock reads noon, so a clocked planet never sleeps")
    void clockDisabled() {
        for (double phase : new double[] {0.25, 0.6, 0.75}) {
            assertEquals(Decision.REFUSE_DAY, PlanetSleepRules.decide(World.CLOCKED, false, false, phase, W, null));
            assertEquals(Decision.REFUSE_DAY, PlanetSleepRules.decide(World.CLOCKED, false, true, phase, W, null));
        }
    }

    @Test
    @DisplayName("Greenxertz is vanilla's: no planet decision")
    void vanillaWorld() {
        assertEquals(Decision.VANILLA, PlanetSleepRules.decide(World.VANILLA, true, false, 0.25, W, null));
        assertFalse(Decision.VANILLA.refused());
    }

    @Test
    @DisplayName("every refusal names a nerospace.sleep.* line; allowances name none")
    void messages() {
        for (Decision d : Decision.values()) {
            if (d.refused()) {
                assertNotNull(d.messageKey());
                assertTrue(d.messageKey().startsWith("nerospace.sleep."), d.name());
            } else {
                assertNull(d.messageKey());
            }
        }
        assertEquals(PlanetSleepRules.KEY_DAY, Decision.REFUSE_DAY.messageKey());
        assertEquals(PlanetSleepRules.KEY_STATION, Decision.REFUSE_STATION.messageKey());
        assertEquals(PlanetSleepRules.KEY_NYXARA_BRIGHT, Decision.REFUSE_NYXARA_BRIGHT.messageKey());
    }

    @Test
    @DisplayName("sleepers needed: vanilla's maths (0 % = one sleeper, 100 % = everyone, 50 % of 3 = 2)")
    void sleepersNeeded() {
        assertEquals(1, PlanetSleepRules.sleepersNeeded(5, 0));
        assertEquals(1, PlanetSleepRules.sleepersNeeded(1, 100));
        assertEquals(3, PlanetSleepRules.sleepersNeeded(3, 100));
        assertEquals(2, PlanetSleepRules.sleepersNeeded(3, 50));
        assertEquals(1, PlanetSleepRules.sleepersNeeded(0, 100), "never zero");
    }

    @Test
    @DisplayName("skips: enough sleepers and enough deep sleepers; over 100 % never; nobody asleep never")
    void skips() {
        assertTrue(PlanetSleepRules.skips(100, 1, 1, 1));
        assertFalse(PlanetSleepRules.skips(100, 1, 1, 0), "not slept long enough yet");
        assertFalse(PlanetSleepRules.skips(100, 2, 1, 1), "the second player is awake");
        assertTrue(PlanetSleepRules.skips(100, 2, 2, 2));
        assertTrue(PlanetSleepRules.skips(50, 3, 2, 2));
        assertFalse(PlanetSleepRules.skips(50, 3, 2, 1));
        assertTrue(PlanetSleepRules.skips(0, 5, 1, 1));
        assertFalse(PlanetSleepRules.skips(101, 1, 1, 1));
        assertFalse(PlanetSleepRules.skips(0, 5, 0, 0));
    }

    @Test
    @DisplayName("every planet uses the default window unless it overrides it")
    void planetWindows() {
        for (PlanetDefinition p : Planets.ALL) {
            assertNotNull(p.sleepWindow(), p.englishName());
        }
        assertEquals(W, Planets.MARS.sleepWindow());
    }

    @Test
    @DisplayName("worlds: Greenxertz vanilla, Nyxara by band, the station never, the moving clocks by phase")
    void worldKinds() {
        assertEquals(World.VANILLA, PlanetSleep.worldOf(Planets.GREENXERTZ));
        assertEquals(World.NYXARA, PlanetSleep.worldOf(Planets.NYXARA));
        assertEquals(World.STATION, PlanetSleep.worldOf(Planets.STATION));
        for (PlanetDefinition p : new PlanetDefinition[] {Planets.LUNA, Planets.MARS, Planets.CINDARA,
                Planets.GLACIRA}) {
            assertEquals(World.CLOCKED, PlanetSleep.worldOf(p), p.englishName());
        }
    }
}
