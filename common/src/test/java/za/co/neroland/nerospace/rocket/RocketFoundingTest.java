package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.rocket.StationFounding.Outcome;

/** 001 §13.2 / §18.2, 005 §8: charter on arrival; 1.6.0 in-flight honoured once; full → nothing consumed. */
class RocketFoundingTest {

    @Test
    void charterRule() {
        assertEquals(Outcome.FOUND_CONSUME, StationFounding.resolve(false, true, false));
        assertEquals(Outcome.REDIRECT, StationFounding.resolve(false, false, false));
        assertEquals(Outcome.FOUND_FREE, StationFounding.resolve(true, false, false));
        assertEquals(Outcome.FOUND_FREE, StationFounding.resolve(true, true, false));
    }

    @Test
    void fullRegistryConsumesNothing() {
        assertEquals(Outcome.REDIRECT_FULL, StationFounding.resolve(false, true, true));
        assertEquals(Outcome.REDIRECT_FULL, StationFounding.resolve(true, false, true));
        assertEquals(Outcome.REDIRECT, StationFounding.resolve(false, false, true));
    }

    @Test
    void parkedLegacyRocketFollowsTheCharterRule() {
        // 1.6.0 save (no CharterRule): parked → 2.0 rule; already in flight → honoured once.
        assertTrue(StationFounding.charterRuleAware(null, false));
        assertFalse(StationFounding.charterRuleAware(null, true));
        // 2.0 saves always carry the flag.
        assertTrue(StationFounding.charterRuleAware(Boolean.TRUE, true));
        assertTrue(StationFounding.charterRuleAware(Boolean.TRUE, false));
        // The parked 1.6.0 rocket with StationSlot -2 and no charter is redirected, not founded for free.
        boolean legacy = !StationFounding.charterRuleAware(null, false);
        assertEquals(Outcome.REDIRECT, StationFounding.resolve(legacy, false, false));
        assertEquals(Outcome.FOUND_CONSUME, StationFounding.resolve(legacy, true, false));
        // The in-flight one founds for free, once.
        boolean inFlight = !StationFounding.charterRuleAware(null, true);
        assertEquals(Outcome.FOUND_FREE, StationFounding.resolve(inFlight, false, false));
    }

    @Test
    void originPlatformIsNeverRestamped() {
        assertTrue(OriginPlatformGuard.alreadyBuilt(false, true, false));
        assertTrue(OriginPlatformGuard.alreadyBuilt(false, false, true));
        assertTrue(OriginPlatformGuard.alreadyBuilt(true, false, false));
        assertFalse(OriginPlatformGuard.alreadyBuilt(false, false, false));
    }
}
