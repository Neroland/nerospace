package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Europa Ice melt. */
class MeltRulesTest {

    @Test
    @DisplayName("only Europa Ice beside a heat source melts")
    void melts() {
        assertTrue(MeltRules.melts(true, true));
        assertFalse(MeltRules.melts(true, false));
        assertFalse(MeltRules.melts(false, true));
        assertFalse(MeltRules.melts(false, false));
    }

    @Test
    @DisplayName("any one of the six neighbours is enough")
    void anyHeat() {
        assertFalse(MeltRules.anyHeat(new boolean[6]));
        for (int i = 0; i < 6; i++) {
            boolean[] heat = new boolean[6];
            heat[i] = true;
            assertTrue(MeltRules.anyHeat(heat));
        }
        assertEquals(6, MeltRules.STEAM_PARTICLES);
    }
}
