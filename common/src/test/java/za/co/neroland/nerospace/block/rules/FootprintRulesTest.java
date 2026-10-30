package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Regolith footprints. */
@SuppressWarnings("unused")
class FootprintRulesTest {

    @Test
    @DisplayName("living walkers imprint on a 0.1 fall or 0.2 move; items, spectators and re-steps do not")
    void imprint() {
        assertTrue(FootprintRules.shouldImprint(true, false, 0.1, 0.0, false));
        assertTrue(FootprintRules.shouldImprint(true, false, 0.0, 0.2, false));
        assertFalse(FootprintRules.shouldImprint(true, false, 0.09, 0.19, false));
        assertFalse(FootprintRules.shouldImprint(false, false, 2.0, 2.0, false), "items/projectiles");
        assertFalse(FootprintRules.shouldImprint(true, true, 2.0, 2.0, false), "spectators");
        assertFalse(FootprintRules.shouldImprint(true, false, 2.0, 2.0, true), "already imprinted short-circuits");
    }

    @Test
    @DisplayName("cleared by an update from above, a piston, or a re-step from the opposite facing")
    void clear() {
        assertTrue(FootprintRules.shouldClear(true, false));
        assertTrue(FootprintRules.shouldClear(false, true));
        assertFalse(FootprintRules.shouldClear(false, false));
        assertTrue(FootprintRules.shouldClear(false, false, true));
        assertFalse(FootprintRules.shouldClear(false, false, false));
    }

    @Test
    @DisplayName("landing puff only from a 1-block fall, 6 particles")
    void puff() {
        assertTrue(FootprintRules.showPuff(1.0));
        assertFalse(FootprintRules.showPuff(0.99));
        assertEquals(6, FootprintRules.PUFF_PARTICLES);
    }
}
