package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Effective radiation: Nyxara band overrides, dust storm, and the flare's +1. */
class RadiationRulesTest {

    @Test
    @DisplayName("Nyxara biomes override the trait: nightside 3, dayside 2, band 1")
    void nyxaraBase() {
        assertEquals(3, RadiationRules.base(3, RadiationRules.NYXARA_NIGHTSIDE));
        assertEquals(2, RadiationRules.base(3, RadiationRules.NYXARA_DAYSIDE));
        assertEquals(1, RadiationRules.base(3, RadiationRules.NYXARA_BAND));
        assertEquals(1, RadiationRules.base(1, "nerospace:mars"));
        assertEquals(3, RadiationRules.base(7, ""));
    }

    @Test
    @DisplayName("Europa: 2 on the open surface, 0 under the ice, 1 on terraformed ground; a storm adds 1 outside")
    void europa() {
        assertEquals(2, Planets.EUROPA.traits().radiation());
        assertEquals(0, RadiationRules.base(2, "nerospace:europa_shell"));
        assertEquals(0, RadiationRules.base(2, "nerospace:europa_abyss"));
        assertEquals(0, RadiationRules.base(2, "nerospace:europa_vent_fields"));
        assertEquals(1, RadiationRules.base(2, "nerospace:terraformed_europa_living"));
        assertEquals(1, RadiationRules.base(2, "nerospace:terraformed_europa_rooted"));
        for (String b : new String[] {"europa_chaos", "europa_ice_plains", "europa_lineae", "europa_penitentes"}) {
            assertEquals(2, RadiationRules.base(2, "nerospace:" + b), b);
        }
        assertEquals(3, RadiationRules.effective(2, WeatherEvent.FLARE, "nerospace:europa_chaos", true));
        assertEquals(2, RadiationRules.effective(2, WeatherEvent.FLARE, "nerospace:europa_chaos", false));
        assertEquals(0, RadiationRules.effective(0, WeatherEvent.FLARE, "nerospace:europa_shell", false));
        assertEquals(2, RadiationRules.effective(1, WeatherEvent.FLARE, "nerospace:terraformed_europa_living", true));
        // Unchanged: ids that merely resemble Europa's do not match, and other planets keep their trait.
        assertEquals(1, RadiationRules.base(1, "nerospace:mars_dunes"));
        assertEquals(3, RadiationRules.base(3, "nerospace:europa"));
    }

    @Test
    @DisplayName("A flare adds +1 on the dayside and in the band, exposed and unsealed only")
    void flarePlusOne() {
        assertEquals(2, RadiationRules.effective(1, WeatherEvent.FLARE, RadiationRules.NYXARA_BAND, true));
        assertEquals(3, RadiationRules.effective(2, WeatherEvent.FLARE, RadiationRules.NYXARA_DAYSIDE, true));
        // nightside faces away from the sun; already at the cap anyway
        assertFalse(RadiationRules.flareApplies(WeatherEvent.FLARE, RadiationRules.NYXARA_NIGHTSIDE, true));
        assertEquals(3, RadiationRules.effective(3, WeatherEvent.FLARE, RadiationRules.NYXARA_NIGHTSIDE, true));
        // sheltered or inside a sealed field: no bonus
        assertEquals(1, RadiationRules.effective(1, WeatherEvent.FLARE, RadiationRules.NYXARA_BAND, false));
        // no flare: no bonus
        assertEquals(1, RadiationRules.effective(1, WeatherEvent.NONE, RadiationRules.NYXARA_BAND, true));
        assertTrue(RadiationRules.flareApplies(WeatherEvent.FLARE, RadiationRules.NYXARA_BAND, true));
    }

    @Test
    @DisplayName("The flare lifts the band to the damage threshold")
    void bandReachesThreshold() {
        int calm = RadiationRules.effective(1, WeatherEvent.NONE, RadiationRules.NYXARA_BAND, true);
        int flare = RadiationRules.effective(1, WeatherEvent.FLARE, RadiationRules.NYXARA_BAND, true);
        assertTrue(calm < RadiationRules.DAMAGE_THRESHOLD);
        assertTrue(flare >= RadiationRules.DAMAGE_THRESHOLD);
    }

    @Test
    @DisplayName("Mars dust storm lifts exposed positions to 2; other events never add radiation")
    void stormAndOthers() {
        assertEquals(2, RadiationRules.effective(1, WeatherEvent.DUST_STORM, "nerospace:mars", true));
        assertEquals(1, RadiationRules.effective(1, WeatherEvent.DUST_STORM, "nerospace:mars", false));
        for (WeatherEvent e : WeatherEvent.values()) {
            if (e != WeatherEvent.DUST_STORM && e != WeatherEvent.FLARE) {
                assertEquals(1, RadiationRules.effective(1, e, RadiationRules.NYXARA_BAND, true), e.name());
            }
        }
    }
}
