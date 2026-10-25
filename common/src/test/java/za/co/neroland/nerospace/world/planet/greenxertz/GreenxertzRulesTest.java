package za.co.neroland.nerospace.world.planet.greenxertz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.block.rules.GrowthRules;
import za.co.neroland.nerospace.world.planet.greenxertz.world.GiantGlowcap;

/** Pure Greenxertz rules: giant glowcap stem sizing, heal-on-wake trigger, bloom particle budget. */
class GreenxertzRulesTest {

    @Test
    void bonemealGiantFitsTheFiveAirGate() {
        // bone meal: always the 4-block stem, which with the cap fills exactly the 5 air cells the gate demands
        assertEquals(GiantGlowcap.STEM, GiantGlowcap.stemHeight(GrowthRules.AIR_NEEDED, 9, false));
        assertTrue(GiantGlowcap.STEM + 1 <= GrowthRules.AIR_NEEDED);
    }

    @Test
    void worldgenStemStaysInsideTheFreeColumn() {
        for (int air = GrowthRules.AIR_NEEDED; air <= GiantGlowcap.MAX_STEM + 1; air++) {
            for (int roll = 0; roll < 16; roll++) {
                int h = GiantGlowcap.stemHeight(air, roll, true);
                assertTrue(h >= GiantGlowcap.STEM && h <= GiantGlowcap.MAX_STEM, "h=" + h);
                assertTrue(h + 1 <= air, "stem + cap fit in " + air + " air");
            }
        }
    }

    @Test
    void healOnlyAfterACompletedSleep() {
        assertTrue(BioBarkHeal.wokeFromFullSleep(true, false));
        assertFalse(BioBarkHeal.wokeFromFullSleep(true, true));
        assertFalse(BioBarkHeal.wokeFromFullSleep(false, false));
    }

    @Test
    void bloomParticlesScaleAndClamp() {
        assertEquals(0, GreenxertzWeather.particles(0.0F));
        assertEquals(GreenxertzWeather.MAX_PARTICLES, GreenxertzWeather.particles(1.0F));
        assertEquals(GreenxertzWeather.MAX_PARTICLES, GreenxertzWeather.particles(3.0F));
        assertEquals(0, GreenxertzWeather.particles(-1.0F));
    }
}
