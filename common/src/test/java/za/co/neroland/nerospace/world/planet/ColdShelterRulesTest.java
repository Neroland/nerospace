package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.ColdShelterRules.Source;

/** Which world's cold shield answers for a planet, and when the 10-tick cache serves a repeat call. */
class ColdShelterRulesTest {

    @Test
    @DisplayName("dispatch: Nyxara keeps its Dawnband lookup, Europa gets its own, every other world has none")
    void dispatch() {
        assertEquals(Source.NYXARA, ColdShelterRules.source(Planets.NYXARA.id().asString()));
        assertEquals(Source.EUROPA, ColdShelterRules.source(Planets.EUROPA.id().asString()));
        for (PlanetDefinition p : Planets.ALL) {
            if (p == Planets.NYXARA || p == Planets.EUROPA) {
                continue;
            }
            // Before 2.4 these worlds asked the Dawnband lookup, which answers false wherever no flare runs; only
            // Nyxara has flares among them, so "no source" is the same answer.
            assertEquals(Source.NONE, ColdShelterRules.source(p.id().asString()), p.langId());
            assertFalse(p.weather().event() == WeatherEvent.FLARE, p.langId() + " has no flare");
        }
        assertEquals(Source.NONE, ColdShelterRules.source("minecraft:overworld"));
        assertEquals(Source.NONE, ColdShelterRules.source(null));
    }

    @Test
    @DisplayName("cache: same position in the same 10-tick bucket hits; anything else misses")
    void cache() {
        assertEquals(10, ColdShelterRules.BUCKET_TICKS);
        assertEquals(0L, ColdShelterRules.bucket(9L));
        assertEquals(1L, ColdShelterRules.bucket(10L));
        assertEquals(1L, ColdShelterRules.bucket(19L));
        assertTrue(ColdShelterRules.cacheHit(true, 42L, 7L, 42L, 7L));
        assertFalse(ColdShelterRules.cacheHit(false, 42L, 7L, 42L, 7L), "nothing cached yet");
        assertFalse(ColdShelterRules.cacheHit(true, 42L, 7L, 43L, 7L), "another position");
        assertFalse(ColdShelterRules.cacheHit(true, 42L, 7L, 42L, 8L), "the next bucket");
    }
}
