package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sulfur Crust. */
@SuppressWarnings("unused")
class IgniteRulesTest {

    @Test
    @DisplayName("at most 5 flammable neighbours, never above the cap")
    void targets() {
        boolean[] all = {true, true, true, true, true, true};
        assertEquals(5, Integer.bitCount(IgniteRules.targets(all, 5)));
        assertEquals(5, Integer.bitCount(IgniteRules.targets(all, 99)));
        assertEquals(2, Integer.bitCount(IgniteRules.targets(all, 2)));
        assertEquals(0, IgniteRules.targets(new boolean[6], 5));
        assertEquals(0b010010, IgniteRules.targets(new boolean[] {false, true, false, false, true, false}, 5));
        assertEquals(30, IgniteRules.FLAMMABILITY);
        assertEquals(60, IgniteRules.FIRE_SPREAD);
    }
}
