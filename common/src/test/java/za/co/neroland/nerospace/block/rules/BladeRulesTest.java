package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Penitente Blade. */
class BladeRulesTest {

    @Test
    @DisplayName("a fall of 2 or more deals 1.5x and breaks the blade")
    void damage() {
        assertEquals(6.0F, BladeRules.damage(2.0, 4.0F));
        assertEquals(4.0F, BladeRules.damage(1.99, 4.0F));
        assertEquals(15.0F, BladeRules.damage(10.0, 10.0F));
        assertTrue(BladeRules.breaks(2.0));
        assertTrue(BladeRules.breaks(8.0));
        assertFalse(BladeRules.breaks(1.99));
        assertFalse(BladeRules.breaks(0.0));
    }

    @Test
    @DisplayName("natives take none")
    void natives() {
        assertEquals(0.0F, BladeRules.damageFactor(5.0, true));
        assertEquals(0.0F, BladeRules.damageFactor(0.5, true));
        assertEquals(1.5F, BladeRules.damageFactor(5.0, false));
        assertEquals(1.0F, BladeRules.damageFactor(0.5, false));
    }
}
