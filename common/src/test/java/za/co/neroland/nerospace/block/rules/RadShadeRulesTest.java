package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sunglass Crystal shade. */
@SuppressWarnings("unused")
class RadShadeRulesTest {

    @Test
    @DisplayName("any shade block in the three cells above the head shades; ambient drops by one, never below 0")
    void shade() {
        assertFalse(RadShadeRules.shaded(new boolean[] {false, false, false}));
        assertTrue(RadShadeRules.shaded(new boolean[] {false, false, true}));
        assertTrue(RadShadeRules.shaded(new boolean[] {true, false, false}));
        assertFalse(RadShadeRules.shaded(new boolean[] {false, false, false, true}), "only three cells count");
        assertFalse(RadShadeRules.shaded(null));
        assertEquals(1, RadShadeRules.effectiveRadiation(2, true), "dayside 2 -> 1");
        assertEquals(2, RadShadeRules.effectiveRadiation(2, false));
        assertEquals(0, RadShadeRules.effectiveRadiation(0, true));
        assertEquals(2, RadShadeRules.effectiveRadiation(3, true), "nightside 3 -> 2");
    }
}
