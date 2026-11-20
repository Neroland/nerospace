package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Smoker Chimney growth. */
class ChimneyRulesTest {

    @Test
    @DisplayName("grows one at a time up to 5, only over an active vent with room")
    void mayGrow() {
        for (int h = 1; h < 5; h++) {
            assertTrue(ChimneyRules.mayGrow(h, true, true));
        }
        assertFalse(ChimneyRules.mayGrow(5, true, true));
        assertFalse(ChimneyRules.mayGrow(6, true, true));
        assertFalse(ChimneyRules.mayGrow(2, false, true));
        assertFalse(ChimneyRules.mayGrow(2, true, false));
        assertFalse(ChimneyRules.mayGrow(0, true, true));
        assertEquals(5, ChimneyRules.MAX);
    }

    @Test
    @DisplayName("8 % per random tick")
    void chance() {
        assertEquals(0.08F, ChimneyRules.GROW_CHANCE);
        assertTrue(ChimneyRules.grows(0.079F));
        assertFalse(ChimneyRules.grows(0.08F));
    }
}
