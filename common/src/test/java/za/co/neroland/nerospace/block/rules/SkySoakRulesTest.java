package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Twilight Crystal. */
@SuppressWarnings("unused")
class SkySoakRulesTest {

    @Test
    @DisplayName("glow follows sky light: 0..8 dark, 9..11 -> 1, 12..14 -> 2, 15 -> 3; light 0/3/5/7")
    void glow() {
        assertEquals(0, SkySoakRules.glow(0));
        assertEquals(0, SkySoakRules.glow(8));
        assertEquals(1, SkySoakRules.glow(9));
        assertEquals(2, SkySoakRules.glow(12));
        assertEquals(3, SkySoakRules.glow(15));
        assertEquals(3, SkySoakRules.glow(99));
        assertEquals(0, SkySoakRules.light(0));
        assertEquals(3, SkySoakRules.light(1));
        assertEquals(5, SkySoakRules.light(2));
        assertEquals(7, SkySoakRules.light(3));
    }
}
