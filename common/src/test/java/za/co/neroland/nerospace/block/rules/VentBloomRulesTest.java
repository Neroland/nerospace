package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tube Mat bloom. */
class VentBloomRulesTest {

    @Test
    @DisplayName("spreads only submerged, near a vent and under the cap")
    void maySpread() {
        assertTrue(VentBloomRules.maySpread(true, true, 0));
        assertTrue(VentBloomRules.maySpread(true, true, 23));
        assertFalse(VentBloomRules.maySpread(true, true, 24));
        assertFalse(VentBloomRules.maySpread(false, true, 0));
        assertFalse(VentBloomRules.maySpread(true, false, 0));
        assertEquals(24, VentBloomRules.CAP);
    }

    @Test
    @DisplayName("one random tick in four attempts, and one scan is 676 reads")
    void cost() {
        assertEquals(0.25F, VentBloomRules.SPREAD_CHANCE);
        assertTrue(VentBloomRules.attempts(0.0F));
        assertTrue(VentBloomRules.attempts(0.249F));
        assertFalse(VentBloomRules.attempts(0.25F));
        assertEquals(676, VentBloomRules.scanReads());
        assertEquals(7, VentBloomRules.LIGHT);
    }
}
