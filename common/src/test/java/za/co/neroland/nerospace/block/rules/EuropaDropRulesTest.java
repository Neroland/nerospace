package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The shared drop transformation: the per-block cases live in the four rule tests. */
class EuropaDropRulesTest {

    @Test
    @DisplayName("silk touch and an empty table result are never changed")
    void untouched() {
        List<EuropaDropRules.Stack> any = List.of(new EuropaDropRules.Stack("nerolandcore:brine_salt", 2));
        for (EuropaDropRules.Kind kind : EuropaDropRules.Kind.values()) {
            assertSame(any, EuropaDropRules.apply(kind, any, true, true, 3));
            assertSame(any, EuropaDropRules.apply(kind, any, true, false, 0));
            assertTrue(EuropaDropRules.apply(kind, List.of(), false, true, 3).isEmpty());
            assertTrue(EuropaDropRules.apply(kind, List.of(), false, false, 0).isEmpty());
        }
    }

    @Test
    @DisplayName("stacks of other items pass through")
    void others() {
        List<EuropaDropRules.Stack> mixed = List.of(new EuropaDropRules.Stack("minecraft:stick", 5),
                new EuropaDropRules.Stack("nerolandcore:brine_salt", 2));
        assertEquals(List.of(new EuropaDropRules.Stack("minecraft:stick", 5),
                new EuropaDropRules.Stack("nerolandcore:brine_salt", 4)),
                EuropaDropRules.apply(EuropaDropRules.Kind.BRINE_SALT_ORE, mixed, false, true, 0));
        assertEquals(4, EuropaDropRules.Kind.values().length);
    }
}
