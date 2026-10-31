package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Nyx Crystal Cluster. */
@SuppressWarnings("unused")
class ClusterLootRulesTest {

    @Test
    @DisplayName("only a full cluster drops: 2 to a pickaxe, 1 otherwise; light 1/2/4/5")
    void loot() {
        for (int s = 0; s < 3; s++) {
            assertEquals(0, ClusterLootRules.count(s, true));
            assertEquals(0, ClusterLootRules.count(s, false));
        }
        assertEquals(2, ClusterLootRules.count(3, true));
        assertEquals(1, ClusterLootRules.count(3, false));
        assertEquals(1, ClusterLootRules.light(0));
        assertEquals(5, ClusterLootRules.light(3));
        assertTrue(ClusterLootRules.height(3) > ClusterLootRules.height(0));
    }
}
