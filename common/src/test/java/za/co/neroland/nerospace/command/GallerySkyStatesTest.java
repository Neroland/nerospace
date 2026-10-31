package za.co.neroland.nerospace.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.command.GallerySkyStates.State;
import za.co.neroland.nerospace.command.GallerySkyStates.View;
import za.co.neroland.nerospace.command.GallerySkyStates.Weather;
import za.co.neroland.nerospace.world.planet.ClockSource;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.WeatherMachine;
import za.co.neroland.nerospace.world.planet.WeatherParams;
import za.co.neroland.nerospace.world.planet.WeatherState;
import za.co.neroland.nerospace.world.planet.cindara.CindaraPlanet;
import za.co.neroland.nerospace.world.planet.luna.LunaPlanet;
import za.co.neroland.nerospace.world.planet.nyxara.worldgen.FixedXBands;

/** The {@code /nerospace gallery sky} state table, its parsing and the Nyxara viewing spots: pure, no server. */
class GallerySkyStatesTest {

    private static final Map<String, List<String>> SPECIFIC = Map.of(
            "luna", List.of("earthrise"),
            "mars", List.of("dust_storm", "blue_sunset"),
            "cindara", List.of("eclipse", "ash_fall"),
            "glacira", List.of("aurora", "diamond_dust", "twilight"),
            "greenxertz", List.of("spore_bloom", "twin_noon"),
            "nyxara", List.of("flare", "galaxy_side", "band", "dayside", "nightside"),
            "station", List.of("planet_night", "sunrise"));

    @Test
    @DisplayName("every world has a row, with dawn, noon, dusk, night and its own states, no duplicates")
    void everyWorld() {
        assertEquals(Planets.ALL.size(), GallerySkyStates.planets().size());
        for (PlanetDefinition p : Planets.ALL) {
            List<String> names = GallerySkyStates.names(p.langId());
            assertTrue(names.containsAll(List.of("dawn", "noon", "dusk", "night")), p.langId() + " " + names);
            assertTrue(names.containsAll(SPECIFIC.get(p.langId())), p.langId() + " " + names);
            assertEquals(names.size(), new HashSet<>(names).size(), p.langId() + " has a duplicate state");
            for (String n : names) {
                assertTrue(n.matches("[a-z_]+"), "a state is one lowercase command word: " + n);
            }
        }
    }

    @Test
    @DisplayName("tab-completion offers only that world's states")
    void suggestionsArePerPlanet() {
        assertFalse(GallerySkyStates.names("luna").contains("dust_storm"));
        assertFalse(GallerySkyStates.names("mars").contains("earthrise"));
        assertFalse(GallerySkyStates.names("station").contains("aurora"));
        assertTrue(GallerySkyStates.names("not_a_planet").isEmpty());
        assertTrue(GallerySkyStates.names(null).isEmpty());
    }

    @Test
    @DisplayName("parsing is exact per world and case-insensitive; unknown words and worlds parse to nothing")
    void parsing() {
        assertEquals("eclipse", GallerySkyStates.find("cindara", "eclipse").orElseThrow().name());
        assertEquals("eclipse", GallerySkyStates.find("CINDARA", "Eclipse").orElseThrow().name());
        assertTrue(GallerySkyStates.find("luna", "eclipse").isEmpty());
        assertTrue(GallerySkyStates.find("mars", "").isEmpty());
        assertTrue(GallerySkyStates.find("mars", null).isEmpty());
        assertTrue(GallerySkyStates.find("pluto", "noon").isEmpty());
    }

    @Test
    @DisplayName("phases are on the clock (0 to 1) and land inside each named window")
    void phases() {
        for (String planet : GallerySkyStates.planets()) {
            for (State s : GallerySkyStates.states(planet)) {
                if (s.setsPhase()) {
                    assertTrue(s.phase() >= 0.0 && s.phase() < 1.0, planet + " " + s);
                }
            }
        }
        double earthrise = phase("luna", "earthrise");
        assertTrue(earthrise >= LunaPlanet.EARTHRISE_START && earthrise <= LunaPlanet.EARTHRISE_END);
        assertTrue(CindaraPlanet.inEclipse(phase("cindara", "eclipse")));
        assertTrue(Planets.GLACIRA.daylight().at(phase("glacira", "aurora")) <= 0.3, "the aurora needs Glacira's dark");
        // Diamond dust may only form in the dark, so its state sits where the weather could start on its own.
        assertTrue(Planets.GLACIRA.weatherMayStart(phase("glacira", "diamond_dust")));
        double twilight = Planets.GLACIRA.daylight().at(phase("glacira", "twilight"));
        assertTrue(twilight > 0.0 && twilight < Planets.GLACIRA.daylight().at(GallerySkyStates.NOON));
        assertEquals(0.25, phase("greenxertz", "twin_noon"), 1.0E-9);
        assertEquals(0.5, phase("mars", "blue_sunset"), 1.0E-9);
    }

    @Test
    @DisplayName("weather states exist only where the world has weather, and start that world's own event")
    void weather() {
        for (PlanetDefinition p : Planets.ALL) {
            for (State s : GallerySkyStates.states(p.langId())) {
                if (s.weather() == Weather.START) {
                    assertTrue(p.weather().hasWeather(), p.langId() + " " + s.name());
                }
            }
        }
        assertEquals(Weather.START, state("mars", "dust_storm").weather());
        assertEquals(Weather.START, state("cindara", "ash_fall").weather());
        assertEquals(Weather.START, state("glacira", "diamond_dust").weather());
        assertEquals(Weather.START, state("greenxertz", "spore_bloom").weather());
        assertEquals(Weather.START, state("nyxara", "flare").weather());
        assertEquals(Weather.CLEAR, state("mars", "noon").weather());
    }

    @Test
    @DisplayName("Nyxara's fixed clock is never set: its states are places; elsewhere every state sets the clock")
    void nyxaraIsPlaces() {
        for (State s : GallerySkyStates.states("nyxara")) {
            assertFalse(s.setsPhase(), s.name());
            assertTrue(s.view() != View.STAY, s.name());
        }
        assertEquals(View.DAYSIDE, state("nyxara", "dayside").view());
        assertEquals(View.BAND, state("nyxara", "band").view());
        assertEquals(View.NIGHTSIDE, state("nyxara", "galaxy_side").view());
        for (PlanetDefinition p : Planets.ALL) {
            if (p == Planets.NYXARA) {
                continue;
            }
            for (State s : GallerySkyStates.states(p.langId())) {
                assertTrue(s.setsPhase(), p.langId() + " " + s.name());
                assertEquals(View.STAY, s.view(), p.langId() + " " + s.name());
            }
        }
        assertEquals(ClockSource.PLANET_CLOCK, Planets.NYXARA.clockSource());
        assertTrue(Planets.NYXARA.traits().fixedClock());
    }

    @Test
    @DisplayName("Nyxara viewing spots: the band keeps you inside it, the sides sit past the ragged terminator")
    void nyxaraViewX() {
        int width = FixedXBands.DEFAULT_WIDTH;
        int half = width / 2;
        int noise = 64;
        assertEquals(100, GallerySkyStates.viewX(View.BAND, 100, width, 0, noise));
        int pulled = GallerySkyStates.viewX(View.BAND, 50_000, width, 0, noise);
        assertTrue(pulled < half - noise, "pulled back into the band: " + pulled);
        int day = GallerySkyStates.viewX(View.DAYSIDE, 0, width, 0, noise);
        int night = GallerySkyStates.viewX(View.NIGHTSIDE, 0, width, 0, noise);
        assertTrue(day < -half - noise, "dayside is west (-X) of the terminator: " + day);
        assertTrue(night > half + noise, "nightside is east (+X) of the terminator: " + night);
        assertEquals(day + 1000, GallerySkyStates.viewX(View.DAYSIDE, 0, width, 1000, noise), "follows the origin");
        assertEquals(1234, GallerySkyStates.viewX(View.STAY, 1234, width, 0, noise));
    }

    @Test
    @DisplayName("a gallery weather start sits at its peak at full strength, then runs the normal state machine")
    void weatherAtPeak() {
        WeatherParams params = Planets.MARS.weather();
        WeatherMachine m = new WeatherMachine();
        m.reachPeak();
        assertEquals(WeatherState.CLEAR, m.state, "nothing active: no-op");
        m.forceStart(params, WeatherMachine.MAX_TARGET, params.maxDurationTicks(), 1L);
        m.reachPeak();
        assertEquals(WeatherState.PEAK, m.state);
        assertEquals(1.0F, m.intensity, 1.0E-6F);
        m.step(params, true, true, () -> 0.5);
        assertEquals(WeatherState.PEAK, m.state, "holds at its peak for its duration");
    }

    @Test
    @DisplayName("every state names a sky look for the sky renderer to key on")
    void skyIds() {
        Set<String> all = new HashSet<>();
        for (String planet : GallerySkyStates.planets()) {
            for (State s : GallerySkyStates.states(planet)) {
                assertFalse(s.sky().isBlank(), planet + " " + s.name());
                all.add(s.sky());
            }
        }
        assertTrue(all.containsAll(Set.of("earthrise", "blue_sunset", "aurora", "eclipse", "twin_noon", "galaxy_side",
                "planet_night", "sunrise", "flare")), all.toString());
    }

    private static State state(String planet, String name) {
        return GallerySkyStates.find(planet, name).orElseThrow();
    }

    private static double phase(String planet, String name) {
        return state(planet, name).phase();
    }
}
