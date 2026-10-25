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
    void originPlatformIsNeverRestamped() {
        assertTrue(OriginPlatformGuard.alreadyBuilt(false, true, false));
        assertTrue(OriginPlatformGuard.alreadyBuilt(false, false, true));
        assertTrue(OriginPlatformGuard.alreadyBuilt(true, false, false));
        assertFalse(OriginPlatformGuard.alreadyBuilt(false, false, false));
    }
}
