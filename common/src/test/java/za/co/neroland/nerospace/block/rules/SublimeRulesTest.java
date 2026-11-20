package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Deuterium Ice sublimation. */
class SublimeRulesTest {

    @Test
    @DisplayName("heat in range means no yield")
    void yields() {
        assertTrue(SublimeRules.yields(false));
        assertFalse(SublimeRules.yields(true));
    }

    @Test
    @DisplayName("the boundary is 4: a source 5 away does not count")
    void boundary() {
        assertTrue(SublimeRules.within(4, 0, 0));
        assertTrue(SublimeRules.within(-4, 4, -4));
        assertFalse(SublimeRules.within(5, 0, 0));
        assertFalse(SublimeRules.within(0, -5, 0));
        assertFalse(SublimeRules.within(0, 0, 5));
        assertEquals(729, SublimeRules.scanReads());
    }

    @Test
    @DisplayName("drops: warm and not silk gives nothing; cold or silk keeps the table's drops")
    void drops() {
        List<EuropaDropRules.Stack> shards = List.of(new EuropaDropRules.Stack("nerospace:deuterium_shard", 2));
        assertEquals(List.of(), EuropaDropRules.apply(EuropaDropRules.Kind.DEUTERIUM_ICE, shards, false, true, 0));
        assertEquals(shards, EuropaDropRules.apply(EuropaDropRules.Kind.DEUTERIUM_ICE, shards, false, false, 0));
        List<EuropaDropRules.Stack> block = List.of(new EuropaDropRules.Stack("nerospace:deuterium_ice", 1));
        assertEquals(block, EuropaDropRules.apply(EuropaDropRules.Kind.DEUTERIUM_ICE, block, true, true, 0));
    }
}
