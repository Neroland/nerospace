package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Mirror Crystal. */
@SuppressWarnings("unused")
class StarlitRulesTest {

    @Test
    @DisplayName("lit only under open sky in the nightside")
    void starlit() {
        assertTrue(StarlitRules.starlit(true, true));
        assertFalse(StarlitRules.starlit(false, true));
        assertFalse(StarlitRules.starlit(true, false));
        assertEquals(5, StarlitRules.light(true));
        assertEquals(0, StarlitRules.light(false));
    }
}
