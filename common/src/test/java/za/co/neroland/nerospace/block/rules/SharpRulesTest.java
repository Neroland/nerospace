package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Shard Drift. */
@SuppressWarnings("unused")
class SharpRulesTest {

    @Test
    @DisplayName("cuts a bare-footed living entity, 1.0 every 20 ticks; any boots stop it")
    void sharp() {
        assertTrue(SharpRules.cuts(true, false));
        assertFalse(SharpRules.cuts(true, true));
        assertFalse(SharpRules.cuts(false, false));
        assertEquals(1.0F, SharpRules.DAMAGE);
        assertTrue(SharpRules.dueThisTick(40));
        assertFalse(SharpRules.dueThisTick(41));
    }
}
