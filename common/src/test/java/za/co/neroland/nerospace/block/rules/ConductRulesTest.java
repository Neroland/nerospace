package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Vent Basalt conduction. */
class ConductRulesTest {

    private static final int[] ALL_COLD = {4, 4, 4, 4, 4, 4};

    @Test
    @DisplayName("1 beside an active vent, min + 1 otherwise, capped at 4")
    void distance() {
        assertEquals(1, ConductRules.distance(ALL_COLD, true));
        assertEquals(4, ConductRules.distance(ALL_COLD, false));
        assertEquals(2, ConductRules.distance(new int[] {4, 1, 4, 3, 4, 4}, false));
        assertEquals(3, ConductRules.distance(new int[] {4, 4, 2, 4, 4, 4}, false));
        assertEquals(4, ConductRules.distance(new int[] {4, 4, 3, 4, 4, 4}, false));
        assertEquals(4, ConductRules.distance(new int[0], false));
    }

    @Test
    @DisplayName("warm up to distance 3")
    void warm() {
        assertTrue(ConductRules.warm(1));
        assertTrue(ConductRules.warm(3));
        assertFalse(ConductRules.warm(4));
        assertEquals(4, ConductRules.COLD);
    }

    @Test
    @DisplayName("the wave dies three hops from the vent")
    void wave() {
        int d = ConductRules.distance(ALL_COLD, true);
        int hops = 0;
        while (ConductRules.warm(d)) {
            d = ConductRules.distance(new int[] {d, 4, 4, 4, 4, 4}, false);
            hops++;
        }
        assertEquals(3, hops);
    }
}
