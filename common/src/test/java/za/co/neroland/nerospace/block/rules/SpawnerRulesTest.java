package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Dust Devil Pit. */
@SuppressWarnings("unused")
class SpawnerRulesTest {

    @Test
    @DisplayName("storm + roll + fewer than 2 nearby")
    void spawn() {
        assertTrue(SpawnerRules.canSpawn(true, 0, 2, true));
        assertTrue(SpawnerRules.canSpawn(true, 1, 2, true));
        assertFalse(SpawnerRules.canSpawn(true, 2, 2, true));
        assertFalse(SpawnerRules.canSpawn(true, 2, 5, true), "cap never raised");
        assertFalse(SpawnerRules.canSpawn(false, 0, 2, true));
        assertFalse(SpawnerRules.canSpawn(true, 0, 2, false));
        assertEquals(600, SpawnerRules.INTERVAL_TICKS);
        assertEquals(3, SpawnerRules.ONE_IN);
        assertEquals(600, SpawnerRules.LIFETIME_TICKS);
    }
}
