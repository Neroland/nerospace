package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Oxidant Ice radiolysis. */
class RadiolysisRulesTest {

    @Test
    @DisplayName("gains only under sky, at radiation 2 or more, below charge 3")
    void gains() {
        assertTrue(RadiolysisRules.gains(true, 2, 0));
        assertTrue(RadiolysisRules.gains(true, 3, 2));
        assertFalse(RadiolysisRules.gains(true, 2, 3));
        assertFalse(RadiolysisRules.gains(true, 1, 0));
        assertFalse(RadiolysisRules.gains(false, 3, 0));
    }

    @Test
    @DisplayName("yield is 1 + charge; full ice stops ticking")
    void yieldAndTicks() {
        assertEquals(1, RadiolysisRules.yield(0));
        assertEquals(2, RadiolysisRules.yield(1));
        assertEquals(4, RadiolysisRules.yield(3));
        assertEquals(4, RadiolysisRules.yield(9));
        assertEquals(1, RadiolysisRules.yield(-1));
        assertTrue(RadiolysisRules.ticks(0));
        assertTrue(RadiolysisRules.ticks(2));
        assertFalse(RadiolysisRules.ticks(3));
    }

    @Test
    @DisplayName("a flare adds one level")
    void level() {
        assertEquals(2, RadiolysisRules.level(2, false));
        assertEquals(3, RadiolysisRules.level(2, true));
        assertEquals(1, RadiolysisRules.level(0, true));
    }

    @Test
    @DisplayName("drops: not silk gives 1 + charge crystals; silk keeps the block")
    void drops() {
        List<EuropaDropRules.Stack> one = List.of(new EuropaDropRules.Stack("nerospace:oxidant_crystal", 1));
        for (int charge = 0; charge <= 3; charge++) {
            assertEquals(List.of(new EuropaDropRules.Stack("nerospace:oxidant_crystal", 1 + charge)),
                    EuropaDropRules.apply(EuropaDropRules.Kind.OXIDANT_ICE, one, false, false, charge));
        }
        List<EuropaDropRules.Stack> block = List.of(new EuropaDropRules.Stack("nerospace:oxidant_ice", 1));
        assertEquals(block, EuropaDropRules.apply(EuropaDropRules.Kind.OXIDANT_ICE, block, true, false, 3));
        assertEquals(List.of(), EuropaDropRules.apply(EuropaDropRules.Kind.OXIDANT_ICE, List.of(), false, false, 3));
    }
}
