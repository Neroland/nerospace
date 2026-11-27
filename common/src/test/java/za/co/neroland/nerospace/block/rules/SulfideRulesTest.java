package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Vent Sulfide Ore. */
class SulfideRulesTest {

    @Test
    @DisplayName("wet from a neighbouring liquid or its own water")
    void wet() {
        assertTrue(SulfideRules.wet(true, false));
        assertTrue(SulfideRules.wet(false, true));
        assertFalse(SulfideRules.wet(false, false));
        assertTrue(SulfideRules.dropsOre(true));
        assertFalse(SulfideRules.dropsOre(false));
    }

    @Test
    @DisplayName("drops: dry and not silk gives one sulfur; wet or silk keeps the table's drops")
    void drops() {
        List<EuropaDropRules.Stack> ore = List.of(new EuropaDropRules.Stack("nerolandcore:raw_vent_sulfide", 2));
        assertEquals(List.of(new EuropaDropRules.Stack("nerospace:sulfur", 1)),
                EuropaDropRules.apply(EuropaDropRules.Kind.VENT_SULFIDE_ORE, ore, false, false, 0));
        assertEquals(ore, EuropaDropRules.apply(EuropaDropRules.Kind.VENT_SULFIDE_ORE, ore, false, true, 0));
        List<EuropaDropRules.Stack> block = List.of(new EuropaDropRules.Stack("nerolandcore:vent_sulfide_ore", 1));
        assertEquals(block, EuropaDropRules.apply(EuropaDropRules.Kind.VENT_SULFIDE_ORE, block, true, false, 0));
        assertEquals(List.of(), EuropaDropRules.apply(EuropaDropRules.Kind.VENT_SULFIDE_ORE, List.of(), false, false, 0));
    }
}
