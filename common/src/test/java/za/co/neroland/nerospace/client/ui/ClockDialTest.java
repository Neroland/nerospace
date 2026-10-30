package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.ClockMath;
import za.co.neroland.nerospace.world.planet.DaylightCurve;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * The clock dial follows the shared phase convention: 0 sunrise = right, 0.25 noon = up, 0.5 sunset = left,
 * 0.75 midnight = down. The planet card's dial used to put phase 0 at the bottom (midnight); it now draws through
 * {@link ClockDial}, so these cover the card sizes (r = 3 compact, r = 5 standard) and the HUD (r = 7, 15).
 */
class ClockDialTest {

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 7, 15 })
    @DisplayName("phase 0 is sunrise (hand right), 0.25 noon (up), 0.5 sunset (left), 0.75 midnight (down)")
    void handFollowsPhaseConvention(int r) {
        double c = ClockDial.centre(r);
        int[] sunrise = ClockDial.tip(r, 0.0);
        int[] noon = ClockDial.tip(r, ClockMath.NOON);
        int[] sunset = ClockDial.tip(r, 0.5);
        int[] midnight = ClockDial.tip(r, 0.75);
        assertTrue(sunrise[0] > c && Math.abs(sunrise[1] - c) <= 0.5, "sunrise points right");
        assertTrue(noon[1] < c && Math.abs(noon[0] - c) <= 0.5, "noon points up");
        assertTrue(sunset[0] < c && Math.abs(sunset[1] - c) <= 0.5, "sunset points left");
        assertTrue(midnight[1] > c && Math.abs(midnight[0] - c) <= 0.5, "midnight points down");
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 5, 7 })
    @DisplayName("the hand stays inside the dial box")
    void handInsideBox(int r) {
        int s = ClockDial.size(r);
        for (int i = 0; i < 64; i++) {
            for (int t = 0; t <= r - 2; t++) {
                int[] p = ClockDial.handPixel(r, i / 64.0, t);
                assertTrue(p[0] >= 0 && p[0] < s && p[1] >= 0 && p[1] < s, "r=" + r + " phase " + i / 64.0);
            }
        }
    }

    @Test
    @DisplayName("the face's lit half is the day half: pixels right of and above centre face phase 0..0.5")
    void faceMatchesConvention() {
        int r = 7;
        int c = (int) Math.round(ClockDial.centre(r));
        assertEquals(0.0, ClockDial.phaseAt(ClockDial.size(r) - 1, (int) ClockDial.centre(r), r), 0.05);
        double up = ClockDial.phaseAt(c, 0, r);
        assertEquals(0.25, up, 0.05);
        double down = ClockDial.phaseAt(c, ClockDial.size(r) - 1, r);
        assertEquals(0.75, down, 0.05);
    }

    @Test
    @DisplayName("phase names: sunrise around 0, day, sunset around 0.5, night, eclipse inside an eclipse window")
    void phaseKeys() {
        List<ClockDial.Window> none = List.of();
        assertEquals("hud.nerospace.clock.sunrise", ClockDial.phaseKey(0.0, none));
        assertEquals("hud.nerospace.clock.sunrise", ClockDial.phaseKey(0.97, none));
        assertEquals("hud.nerospace.clock.day", ClockDial.phaseKey(0.25, none));
        assertEquals("hud.nerospace.clock.sunset", ClockDial.phaseKey(0.5, none));
        assertEquals("hud.nerospace.clock.night", ClockDial.phaseKey(0.75, none));
        List<ClockDial.Window> eclipse = List.of(new ClockDial.Window(0.40, 0.50, ClockDial.BAD, true));
        assertEquals("hud.nerospace.clock.eclipse", ClockDial.phaseKey(0.45, eclipse));
        assertEquals("hud.nerospace.clock.day", ClockDial.phaseKey(0.30, eclipse));
    }

    @Test
    @DisplayName("a window that wraps past phase 1 contains both ends")
    void wrappingWindow() {
        ClockDial.Window w = new ClockDial.Window(0.9, 0.1, ClockDial.VALUE, false);
        assertTrue(w.contains(0.95));
        assertTrue(w.contains(0.05));
        assertTrue(!w.contains(0.5));
    }

    @Test
    @DisplayName("Luna names Earthrise inside 0.78-0.86 and only there")
    void lunaEarthrise() {
        List<ClockDial.Window> w = ClockDial.windowsFor(Planets.LUNA, null);
        assertEquals("hud.nerospace.clock.earthrise", ClockDial.phaseKey(0.78, w));
        assertEquals("hud.nerospace.clock.earthrise", ClockDial.phaseKey(0.82, w));
        assertEquals("hud.nerospace.clock.night", ClockDial.phaseKey(0.86, w), "end is exclusive");
        assertEquals("hud.nerospace.clock.night", ClockDial.phaseKey(0.77, w));
        assertEquals("hud.nerospace.clock.day", ClockDial.phaseKey(0.25, w));
        assertEquals("hud.nerospace.clock.sunrise", ClockDial.phaseKey(0.0, w));
    }

    @Test
    @DisplayName("Glacira names its two long twilights (the dawn and dusk ramps of its daylight curve)")
    void glaciraTwilight() {
        List<ClockDial.Window> w = ClockDial.windowsFor(Planets.GLACIRA, null);
        for (double p : new double[] { 0.95, 0.99, 0.0, 0.04, 0.45, 0.5, 0.54 }) {
            assertEquals("hud.nerospace.clock.twilight", ClockDial.phaseKey(p, w), "phase " + p);
        }
        assertEquals("hud.nerospace.clock.day", ClockDial.phaseKey(0.25, w));
        assertEquals("hud.nerospace.clock.night", ClockDial.phaseKey(0.75, w));
        // The windows sit exactly on the ramps: full plateau just inside, night floor just outside.
        DaylightCurve curve = Planets.GLACIRA.daylight();
        assertEquals(0.75, curve.at(0.06), 1e-9, "plateau after dawn");
        assertEquals(0.75, curve.at(0.44), 1e-9, "plateau before dusk");
        assertEquals(0.12, curve.at(0.56), 1e-9, "night floor after dusk");
        assertEquals(0.12, curve.at(0.94), 1e-9, "night floor before dawn");
    }

    @Test
    @DisplayName("planets without named windows keep the plain names; Nyxara has no running clock to name")
    void otherPlanets() {
        for (PlanetDefinition p : List.of(Planets.MARS, Planets.GREENXERTZ, Planets.CINDARA, Planets.NYXARA,
                Planets.STATION)) {
            assertTrue(ClockDial.windowsFor(p, null).isEmpty(), p.id().toString());
        }
        assertTrue(Planets.NYXARA.traits().fixedClock(), "Nyxara's clock is pinned (no HUD dial, no label)");
        assertTrue(ClockDial.windowsFor(null, null).isEmpty());
    }

    @Test
    @DisplayName("an eclipse window still reads Eclipse ahead of any other name")
    void eclipseFirst() {
        List<ClockDial.Window> w = List.of(new ClockDial.Window(0.40, 0.50, ClockDial.BAD, true),
                new ClockDial.Window(0.45, 0.55, ClockDial.VALUE, false, ClockDial.TWILIGHT));
        assertEquals("hud.nerospace.clock.eclipse", ClockDial.phaseKey(0.46, w));
        assertEquals("hud.nerospace.clock.twilight", ClockDial.phaseKey(0.52, w));
        assertEquals(null, new ClockDial.Window(0.1, 0.2, ClockDial.VALUE, false).labelKey(), "unnamed by default");
    }
}
