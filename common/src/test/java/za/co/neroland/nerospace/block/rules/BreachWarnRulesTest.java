package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Shell Ice breach warning. */
class BreachWarnRulesTest {

    @Test
    @DisplayName("warns for a liquid 1 to 6 away")
    void warns() {
        assertFalse(BreachWarnRules.warns(0));
        assertTrue(BreachWarnRules.warns(1));
        assertTrue(BreachWarnRules.warns(6));
        assertFalse(BreachWarnRules.warns(7));
    }

    @Test
    @DisplayName("the pitch rises as the distance falls")
    void pitch() {
        for (int d = 1; d < BreachWarnRules.RANGE; d++) {
            assertTrue(BreachWarnRules.pitch(d) > BreachWarnRules.pitch(d + 1));
        }
        assertEquals(0.6F, BreachWarnRules.pitch(6), 1.0e-6F);
        assertEquals(1.35F, BreachWarnRules.pitch(1), 1.0e-6F);
    }

    @Test
    @DisplayName("the nearest axis wins, and none is 0")
    void nearest() {
        assertEquals(0, BreachWarnRules.nearest(new int[6]));
        assertEquals(2, BreachWarnRules.nearest(new int[] {0, 5, 0, 2, 6, 0}));
        assertEquals(4, BreachWarnRules.PARTICLES);
    }
}
