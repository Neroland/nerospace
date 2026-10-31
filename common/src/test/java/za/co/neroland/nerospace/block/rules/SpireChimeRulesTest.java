package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Crystal Spire. */
@SuppressWarnings("unused")
class SpireChimeRulesTest {

    @Test
    @DisplayName("pitch 0.5 + 0.1 per block below, capped at 2.0; once per 10 ticks")
    void chime() {
        assertEquals(0.5F, SpireChimeRules.pitch(0), 1e-6);
        assertEquals(1.0F, SpireChimeRules.pitch(5), 1e-6);
        assertEquals(2.0F, SpireChimeRules.pitch(15), 1e-6);
        assertEquals(2.0F, SpireChimeRules.pitch(40), 1e-6);
        assertTrue(SpireChimeRules.cooldownOk(0, 10));
        assertFalse(SpireChimeRules.cooldownOk(0, 9));
    }
}
