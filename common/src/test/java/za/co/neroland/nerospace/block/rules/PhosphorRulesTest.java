package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Europium Lamp. */
class PhosphorRulesTest {

    @Test
    @DisplayName("14 lit, 0 unlit, never 15")
    void light() {
        assertEquals(14, PhosphorRules.light(true));
        assertEquals(0, PhosphorRules.light(false));
        assertTrue(PhosphorRules.light(true) < 15);
        assertEquals(14, PhosphorRules.NEAR_MISS_OF_FULL);
    }

    @Test
    @DisplayName("use flips the state")
    void toggle() {
        assertTrue(PhosphorRules.toggle(false));
        assertFalse(PhosphorRules.toggle(true));
        assertTrue(PhosphorRules.toggle(PhosphorRules.toggle(true)));
    }
}
