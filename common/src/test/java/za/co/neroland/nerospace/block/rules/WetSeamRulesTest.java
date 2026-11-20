package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Brine Salt Ore wet seam. */
class WetSeamRulesTest {

    @Test
    @DisplayName("x2 beside brine, x1 otherwise, capped at 8")
    void count() {
        assertEquals(2, WetSeamRules.multiplier(true));
        assertEquals(1, WetSeamRules.multiplier(false));
        assertEquals(4, WetSeamRules.count(2, 2));
        assertEquals(6, WetSeamRules.count(3, 2));
        assertEquals(3, WetSeamRules.count(3, 1));
        assertEquals(8, WetSeamRules.count(5, 2));
        assertEquals(8, WetSeamRules.count(12, 1));
        assertEquals(0, WetSeamRules.count(0, 2));
        assertTrue(WetSeamRules.count(64, 2) <= WetSeamRules.CAP);
    }

    @Test
    @DisplayName("drops: wet and not silk doubles each salt stack; dry or silk keeps the table's drops")
    void drops() {
        List<EuropaDropRules.Stack> salt = List.of(new EuropaDropRules.Stack("nerospace:brine_salt", 3));
        assertEquals(List.of(new EuropaDropRules.Stack("nerospace:brine_salt", 6)),
                EuropaDropRules.apply(EuropaDropRules.Kind.BRINE_SALT_ORE, salt, false, true, 0));
        assertEquals(salt, EuropaDropRules.apply(EuropaDropRules.Kind.BRINE_SALT_ORE, salt, false, false, 0));
        List<EuropaDropRules.Stack> fortune = List.of(new EuropaDropRules.Stack("nerospace:brine_salt", 7));
        assertEquals(List.of(new EuropaDropRules.Stack("nerospace:brine_salt", 8)),
                EuropaDropRules.apply(EuropaDropRules.Kind.BRINE_SALT_ORE, fortune, false, true, 0));
        List<EuropaDropRules.Stack> block = List.of(new EuropaDropRules.Stack("nerospace:brine_salt_ore", 1));
        assertEquals(block, EuropaDropRules.apply(EuropaDropRules.Kind.BRINE_SALT_ORE, block, true, true, 0));
        assertFalse(EuropaDropRules.apply(EuropaDropRules.Kind.BRINE_SALT_ORE, salt, false, true, 0).isEmpty());
    }
}
