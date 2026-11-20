package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hydrothermal Vent warmth. */
class VentWarmthRulesTest {

    @Test
    @DisplayName("active only under liquid")
    void active() {
        assertTrue(VentWarmthRules.active(true));
        assertFalse(VentWarmthRules.active(false));
    }

    @Test
    @DisplayName("shields within 5 blocks when active")
    void shielded() {
        assertTrue(VentWarmthRules.shielded(true, 0.0));
        assertTrue(VentWarmthRules.shielded(true, 5.0));
        assertFalse(VentWarmthRules.shielded(true, 5.01));
        assertFalse(VentWarmthRules.shielded(false, 1.0));
        assertEquals(5.0, VentWarmthRules.RADIUS);
    }

    @Test
    @DisplayName("light 7 when active, 0 when dry")
    void light() {
        assertEquals(7, VentWarmthRules.light(true));
        assertEquals(0, VentWarmthRules.light(false));
    }
}
