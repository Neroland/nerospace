package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Budding Nyx Crystal. */
@SuppressWarnings("unused")
class BuddingRulesTest {

    @Test
    @DisplayName("grows 1 in 5, 4 in 5 during a flare; stages -1 -> 0 -> 3 and stops")
    void budding() {
        int normal = 0;
        int flare = 0;
        for (int roll = 0; roll < BuddingRules.ROLL; roll++) {
            normal += BuddingRules.grows(roll, false) ? 1 : 0;
            flare += BuddingRules.grows(roll, true) ? 1 : 0;
        }
        assertEquals(1, normal);
        assertEquals(4, flare);
        assertEquals(0, BuddingRules.next(-1));
        assertEquals(1, BuddingRules.next(0));
        assertEquals(3, BuddingRules.next(2));
        assertEquals(3, BuddingRules.next(3));
    }
}
