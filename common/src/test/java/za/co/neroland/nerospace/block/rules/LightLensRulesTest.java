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

/** Crater Glass lens. */
@SuppressWarnings("unused")
class LightLensRulesTest {

    @Test
    @DisplayName("brightest non-lens neighbour + 1, clamped to 15; dark stays 0")
    void lens() {
        boolean[] none = new boolean[6];
        assertEquals(13, LightLensRules.lensLevel(new int[] {12, 0, 0, 0, 0, 0}, none));
        assertEquals(15, LightLensRules.lensLevel(new int[] {15, 0, 0, 0, 0, 0}, none));
        assertEquals(15, LightLensRules.lensLevel(new int[] {14, 3, 0, 0, 0, 0}, none));
        assertEquals(0, LightLensRules.lensLevel(new int[6], none));
    }

    @Test
    @DisplayName("another lens is not a source (no chains)")
    void noChains() {
        boolean[] lens = {true, false, false, false, false, false};
        assertEquals(4, LightLensRules.lensLevel(new int[] {15, 3, 0, 0, 0, 0}, lens));
        assertEquals(0, LightLensRules.lensLevel(new int[] {15, 0, 0, 0, 0, 0}, lens));
    }

    @Test
    @DisplayName("+2 over vanilla glass (which passes -1)")
    void relativeToGlass() {
        int source = 10;
        int glass = source - 1;
        assertEquals(glass + 2, LightLensRules.lensLevel(new int[] {source, 0, 0, 0, 0, 0}, new boolean[6]));
        assertTrue(LightLensRules.sparkles(8));
        assertFalse(LightLensRules.sparkles(7));
    }
}
