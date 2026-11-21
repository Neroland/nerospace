package za.co.neroland.nerospace.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.village.VillagerCulture;
import za.co.neroland.nerospace.village.VillagerCultures;

/** {@code AlienVillager.Planet} is persisted by ordinal — append only; culture is derived from it. */
class AlienVillagerPlanetOrdinalTest {

    @Test
    void ordinalsAreFrozen() {
        AlienVillager.Planet[] v = AlienVillager.Planet.values();
        assertEquals("GREENXERTZ", v[0].name());
        assertEquals("CINDARA", v[1].name());
        assertEquals("GLACIRA", v[2].name());
        assertEquals("NYXARA", v[3].name());
        // 2.4.0 appended Europa's Brinekin.
        assertEquals("EUROPA", v[4].name());
        assertEquals(5, v.length);
    }

    @Test
    void unknownOrdinalDegradesInsteadOfCrashing() {
        // In range: the stored species.
        AlienVillager.Planet[] v = AlienVillager.Planet.values();
        for (int i = 0; i < v.length; i++) {
            assertSame(v[i], AlienVillager.Planet.byOrdinal(i));
        }
        assertSame(AlienVillager.Planet.EUROPA, AlienVillager.Planet.byOrdinal(4));
        assertSame(AlienVillager.Planet.CINDARA, AlienVillager.Planet.byOrdinal(1));
        // Out of range: always the read default, however many species are appended later.
        assertSame(AlienVillager.Planet.GREENXERTZ, AlienVillager.Planet.byOrdinal(-1));
        assertSame(AlienVillager.Planet.GREENXERTZ, AlienVillager.Planet.byOrdinal(5));
        assertSame(AlienVillager.Planet.GREENXERTZ, AlienVillager.Planet.byOrdinal(99));
        assertSame(AlienVillager.Planet.GREENXERTZ, AlienVillager.Planet.byOrdinal(Integer.MIN_VALUE));
    }

    @Test
    void cultureFollowsTheStoredSpecies() {
        assertEquals("nerospace:xertz", AlienVillager.Planet.GREENXERTZ.culture().toString());
        assertEquals("nerospace:ember_kin", AlienVillager.Planet.CINDARA.culture().toString());
        assertEquals("nerospace:frostbound", AlienVillager.Planet.GLACIRA.culture().toString());
        assertEquals("nerospace:duskborn", AlienVillager.Planet.NYXARA.culture().toString());
        assertEquals("nerospace:brinekin", AlienVillager.Planet.EUROPA.culture().toString());
        for (AlienVillager.Planet p : AlienVillager.Planet.values()) {
            VillagerCulture c = VillagerCultures.byLegacy(p);
            assertSame(p, c.legacy());
            assertEquals(p.culture().toString(), c.id());
            assertSame(p, AlienVillager.Planet.byCulture(p.culture()));
        }
    }

    @Test
    void megaCitiesOnlyOnGreenxertzAndNyxaraAndDuskbornKeepToTheBand() {
        assertEquals(true, VillagerCultures.XERTZ.megaCity());
        assertEquals(true, VillagerCultures.DUSKBORN.megaCity());
        assertEquals(false, VillagerCultures.EMBER_KIN.megaCity());
        assertEquals(false, VillagerCultures.FROSTBOUND.megaCity());
        assertEquals(false, VillagerCultures.BRINEKIN.megaCity());
        for (VillagerCulture c : VillagerCultures.ALL) {
            assertEquals(c == VillagerCultures.DUSKBORN, c.bandOnly(), c.id());
        }
    }
}
