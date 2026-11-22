package za.co.neroland.nerospace.world.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The non-living scan may visit fewer entities; the correction those entities get does not change. */
class GravityScanRulesTest {

    @Test
    @DisplayName("reach covers the far corner of a chunk at the simulation distance")
    void reachCoversTheTickingColumn() {
        assertEquals(32, GravityScanRules.horizontalReach(0));
        assertEquals(10 * 16 + 32, GravityScanRules.horizontalReach(10));
        assertEquals(32, GravityScanRules.horizontalReach(-4));
    }

    @Test
    @DisplayName("grounded, weightless and submerged entities are not corrected")
    void skipMatchesTheOldGuards() {
        assertTrue(GravityScanRules.skipMotion(true, false, false));
        assertTrue(GravityScanRules.skipMotion(false, true, false));
        assertTrue(GravityScanRules.skipMotion(false, false, true));
        assertFalse(GravityScanRules.skipMotion(false, false, false));
    }

    @Test
    @DisplayName("factor 1 and zero gravity leave vertical speed alone; other factors scale it")
    void correctionFormula() {
        assertEquals(0.4D, GravityScanRules.correctedDy(0.4D, 0.08D, 1.0D));
        assertEquals(-0.2D, GravityScanRules.correctedDy(-0.2D, 0.0D, 0.17D));
        // Vanilla already subtracted `gravity`. Putting back gravity * (1 - factor) leaves factor * gravity.
        assertEquals(-0.2D + 0.08D * (1.0D - 0.17D), GravityScanRules.correctedDy(-0.2D, 0.08D, 0.17D));
        assertEquals(0.1D + 0.08D * (1.0D - 1.5D), GravityScanRules.correctedDy(0.1D, 0.08D, 1.5D));
    }
}
