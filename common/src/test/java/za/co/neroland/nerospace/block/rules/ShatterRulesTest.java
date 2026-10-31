package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Crazed Crystal. */
@SuppressWarnings("unused")
class ShatterRulesTest {

    @Test
    @DisplayName("an impact from 3+ shatters natural crystal only; the cascade stops at the cap")
    void shatter() {
        assertTrue(ShatterRules.shatters(3.0, true, false));
        assertFalse(ShatterRules.shatters(2.99, true, false));
        assertFalse(ShatterRules.shatters(10.0, false, false), "an item or arrow never shatters it");
        assertFalse(ShatterRules.shatters(10.0, true, true), "player-placed never shatters");
        assertArrayEquals(new boolean[] {true, false, true, false, false, false},
                ShatterRules.cascade(new boolean[] {true, false, true, false, false, false}, 7));
        assertArrayEquals(new boolean[] {true, false, true, false, false, false},
                ShatterRules.cascade(new boolean[] {true, false, true, true, true, true}, 2));
        assertArrayEquals(new boolean[] {false, false}, ShatterRules.cascade(new boolean[] {true, true}, 0));
        assertEquals(8, ShatterRules.CAP);
        assertEquals(0.5F, ShatterRules.fallDamageFactor());
    }
}
