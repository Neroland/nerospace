package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Dry Ice one-shot neighbour freeze. */
@SuppressWarnings("unused")
class DryIceRulesTest {

    @Test
    @DisplayName("freezes only adjacent sources, at most 6, as a mask")
    void freeze() {
        assertEquals(0, DryIceRules.freezeTargets(new boolean[6]));
        assertEquals(0b100101, DryIceRules.freezeTargets(new boolean[] {true, false, true, false, false, true}));
        assertEquals(0b111111, DryIceRules.freezeTargets(new boolean[] {true, true, true, true, true, true, true, true}));
        assertEquals(6, Integer.bitCount(DryIceRules.freezeTargets(new boolean[] {true, true, true, true, true, true})));
    }

    @Test
    @DisplayName("sublimates above light 11, vanishes 1 in 12")
    void sublimate() {
        assertTrue(DryIceRules.sublimates(12));
        assertFalse(DryIceRules.sublimates(11));
        assertTrue(DryIceRules.vanishes(15, 0));
        assertFalse(DryIceRules.vanishes(15, 1));
        assertFalse(DryIceRules.vanishes(11, 0));
        assertEquals(12, DryIceRules.VANISH_ONE_IN);
    }
}
