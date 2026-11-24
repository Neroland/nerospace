package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.client.sky.SkyProfile;
import za.co.neroland.nerospace.client.sky.SkyScene;

/** The daily eclipse: the sky profile, the planet constants and the renderer's own body angles agree. */
class EuropaEclipseTest {

    private static SkyProfile profile() throws IOException {
        JsonObject json = EuropaFiles.json(EuropaFiles.assets("sky/europa.json"));
        return SkyProfile.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
    }

    private static SkyProfile.Body jupiter(SkyProfile p) {
        return p.bodies().stream().filter(b -> b.id().equals("jupiter")).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("the profile's eclipse window is the planet's")
    void windowMatches() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        SkyProfile.Eclipse e = profile().eclipse().orElseThrow();
        assertEquals("jupiter", e.body());
        assertEquals(EuropaPlanet.ECLIPSE_START, e.phaseStart(), 1.0E-6);
        assertEquals(EuropaPlanet.ECLIPSE_END, e.phaseEnd(), 1.0E-6);
        assertEquals(EuropaPlanet.ECLIPSE_CENTRE, (EuropaPlanet.ECLIPSE_START + EuropaPlanet.ECLIPSE_END) / 2, 1.0E-9);
        assertEquals(EuropaPlanet.ECLIPSE_HALF_WIDTH, (EuropaPlanet.ECLIPSE_END - EuropaPlanet.ECLIPSE_START) / 2,
                1.0E-9);
    }

    @Test
    @DisplayName("the sun is fully behind Jupiter exactly inside the window (the renderer's own angles)")
    void sunHiddenInsideWindow() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        SkyProfile p = profile();
        SkyProfile.Body sun = p.sun().get(0);
        SkyProfile.Body jupiter = jupiter(p);
        assertTrue(jupiter.fixedAngle().isPresent(), "Jupiter does not move");
        assertEquals(1.0F, sun.orbitPeriodDays(), "one pass per Europa day");
        double hidden = jupiter.size() / 2.0 - sun.size() / 2.0;
        assertEquals(10.8, hidden, 1.0E-4);
        float[] j = SkyScene.bodyAngles(jupiter, 0.0);
        float[] jd = SkyScene.direction(j[0], j[1]);
        for (int i = 0; i < 1000; i++) {
            double phase = i / 1000.0;
            float[] s = SkyScene.bodyAngles(sun, phase);
            float[] sd = SkyScene.direction(s[0], s[1]);
            double dot = sd[0] * jd[0] + sd[1] * jd[1] + sd[2] * jd[2];
            double separation = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
            if (phase >= 0.187 && phase <= 0.313) {
                assertTrue(separation < hidden, "hidden at " + phase + ": " + separation);
            }
            if (phase < 0.185 || phase > 0.315) {
                assertTrue(separation > hidden, "visible at " + phase + ": " + separation);
            }
        }
    }

    @Test
    @DisplayName("inEclipse, the tail window around it, and a darker noon than morning")
    void windows() {
        assertTrue(EuropaPlanet.inEclipse(0.25));
        assertFalse(EuropaPlanet.inEclipse(0.18));
        assertFalse(EuropaPlanet.inEclipse(0.32));
        assertTrue(EuropaPlanet.inEclipse(1.25), "phases wrap");
        assertTrue(EuropaPlanet.TAIL_START < EuropaPlanet.ECLIPSE_START
                && EuropaPlanet.TAIL_END > EuropaPlanet.ECLIPSE_END, "the tail window contains the eclipse");
        assertTrue(EuropaPlanet.daylightAt(0.25) < EuropaPlanet.daylightAt(0.10));
        assertEquals(EuropaPlanet.JUPITER_SHINE, EuropaPlanet.daylightAt(0.75), 1.0E-9, "night floor");
        assertTrue(EuropaPlanet.daylightAt(0.12) <= EuropaPlanet.PEAK + 1.0E-9);
    }
}
