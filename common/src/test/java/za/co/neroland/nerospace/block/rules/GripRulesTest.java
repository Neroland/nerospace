package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Cut Europa Ice grip. */
class GripRulesTest {

    @Test
    @DisplayName("cut ice is 15 % faster, other blocks are unchanged")
    void speed() {
        assertEquals(1.15F, GripRules.speedFactor(true));
        assertEquals(1.0F, GripRules.speedFactor(false));
        assertTrue(GripRules.speedFactor(true) > GripRules.speedFactor(false));
    }

    @Test
    @DisplayName("no slide: friction stays at the default")
    void friction() {
        assertEquals(0.6F, GripRules.friction(true));
        assertEquals(GripRules.friction(false), GripRules.friction(true));
        assertFalse(GripRules.friction(true) > 0.6F);
    }
}
