package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Red Regolith dune drift and its caps. */
@SuppressWarnings("unused")
class DriftRulesTest {

    @Test
    @DisplayName("drifts only in a storm, worldgen, air above, into supported air, under the cap")
    void drift() {
        assertTrue(DriftRules.canDrift(true, false, true, true, true, 0, 16));
        assertFalse(DriftRules.canDrift(false, false, true, true, true, 0, 16), "storm only");
        assertFalse(DriftRules.canDrift(true, true, true, true, true, 0, 16), "placed never drifts");
        assertFalse(DriftRules.canDrift(true, false, false, true, true, 0, 16));
        assertFalse(DriftRules.canDrift(true, false, true, false, true, 0, 16), "never buries");
        assertFalse(DriftRules.canDrift(true, false, true, true, false, 0, 16), "needs support");
        assertFalse(DriftRules.canDrift(true, false, true, true, true, 16, 16), "16 per storm");
    }

    @Test
    @DisplayName("config may lower the cap, never raise it")
    void caps() {
        assertFalse(DriftRules.canDrift(true, false, true, true, true, 16, 100));
        assertFalse(DriftRules.canDrift(true, false, true, true, true, 3, 3));
        assertTrue(DriftRules.canDrift(true, false, true, true, true, 2, 3));
        assertFalse(DriftRules.canDrift(true, false, true, true, true, 0, 0));
        assertEquals(100, DriftRules.INTERVAL_TICKS);
        assertEquals(1, DriftRules.MOVES_PER_INTERVAL);
    }

    @Test
    @DisplayName("storm direction is fixed per event seed and in 0..3")
    void direction() {
        for (long seed : new long[] {0L, 1L, 12345L, Long.MAX_VALUE, -7L}) {
            int d = DriftRules.stormDirection(seed);
            assertTrue(d >= 0 && d < 4);
            assertEquals(d, DriftRules.stormDirection(seed));
        }
    }
}
