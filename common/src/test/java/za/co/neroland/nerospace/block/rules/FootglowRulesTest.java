package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Rose Crystal. */
@SuppressWarnings("unused")
class FootglowRulesTest {

    @Test
    @DisplayName("players light it (not mobs, not spectators, not twice); 9 for 60 ticks, idle 2")
    void footglow() {
        assertTrue(FootglowRules.lightUp(true, false, false));
        assertFalse(FootglowRules.lightUp(false, false, false));
        assertFalse(FootglowRules.lightUp(true, true, false));
        assertFalse(FootglowRules.lightUp(true, false, true));
        assertEquals(9, FootglowRules.light(true));
        assertEquals(2, FootglowRules.light(false));
        assertEquals(60, FootglowRules.LIT_TICKS);
    }
}
