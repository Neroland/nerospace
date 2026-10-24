package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.4 Methane Ice, §11.5. */
@SuppressWarnings("unused")
class MethaneRulesTest {

    @Test
    @DisplayName("flame within 2 with chain budget left")
    void ignites() {
        assertTrue(MethaneRules.ignites(0, true));
        assertTrue(MethaneRules.ignites(2, true));
        assertFalse(MethaneRules.ignites(3, true));
        assertFalse(MethaneRules.ignites(-1, true), "no flame");
        assertFalse(MethaneRules.ignites(1, false), "chain cap");
    }

    @Test
    @DisplayName("chain cap 4 per chunk per 100 ticks; config may lower")
    void chain() {
        assertTrue(MethaneRules.chainBudgetLeft(3, 4));
        assertFalse(MethaneRules.chainBudgetLeft(4, 4));
        assertFalse(MethaneRules.chainBudgetLeft(4, 10), "never raised");
        assertFalse(MethaneRules.chainBudgetLeft(1, 1));
        assertEquals(2.0F, MethaneRules.POWER);
        assertEquals(100, MethaneRules.CHAIN_WINDOW_TICKS);
        assertEquals(40, MethaneRules.SCAN_INTERVAL_TICKS);
        int side = 2 * MethaneRules.SCAN_RADIUS + 1;
        assertEquals(125, side * side * side, "5x5x5 scan bound");
    }
}
