package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Brine. */
class BrineRulesTest {

    @Test
    @DisplayName("drag 0.7, lift capped at 0.04 per tick")
    void motion() {
        assertEquals(0.7, BrineRules.drag());
        assertEquals(0.0, BrineRules.buoyancy(0.0));
        assertEquals(0.02, BrineRules.buoyancy(0.5), 1.0e-9);
        assertEquals(0.04, BrineRules.buoyancy(1.0), 1.0e-9);
        assertEquals(0.04, BrineRules.buoyancy(7.0), 1.0e-9);
        assertEquals(0.0, BrineRules.buoyancy(-1.0));
    }

    @Test
    @DisplayName("submerged fraction is clamped to 0..1")
    void submerged() {
        assertEquals(0.0, BrineRules.submergedFraction(11.0, 1.8, 10.875));
        assertEquals(1.0, BrineRules.submergedFraction(5.0, 1.8, 10.875));
        assertEquals(0.5, BrineRules.submergedFraction(10.0, 1.0, 10.5), 1.0e-9);
        assertEquals(0.0, BrineRules.submergedFraction(10.0, 0.0, 10.5));
    }

    @Test
    @DisplayName("never freezes, puts fire out, resets the fall")
    void flags() {
        assertFalse(BrineRules.freezes());
        assertTrue(BrineRules.extinguishes());
        assertTrue(BrineRules.resetsFall());
    }
}
