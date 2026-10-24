package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §3 capped spreading + §1.5 Spore Moss + §1.6 Shadow Lichen wrappers. */
@SuppressWarnings("unused")
class SpreadRulesTest {

    @Test
    @DisplayName("generic: substrate, under cap, roll below boost")
    void generic() {
        assertTrue(SpreadRules.canSpread(true, 0, 48, 1, 0));
        assertFalse(SpreadRules.canSpread(true, 0, 48, 1, 1));
        assertTrue(SpreadRules.canSpread(true, 0, 48, 3, 2));
        assertFalse(SpreadRules.canSpread(true, 0, 48, 3, 3));
        assertFalse(SpreadRules.canSpread(false, 0, 48, 1, 0));
        assertFalse(SpreadRules.canSpread(true, 48, 48, 1, 0));
        assertFalse(SpreadRules.canSpread(true, 0, 48, 1, -1));
    }

    @Test
    @DisplayName("spore moss: 1 in 8, x3 in bloom, cap 48 (config lowers only)")
    void moss() {
        assertTrue(SporeSpreadRules.canSpread(true, 47, 48, false, 0));
        assertFalse(SporeSpreadRules.canSpread(true, 48, 48, false, 0));
        assertFalse(SporeSpreadRules.canSpread(true, 48, 100, false, 0), "never raised");
        assertFalse(SporeSpreadRules.canSpread(true, 10, 10, false, 0), "lowered");
        assertFalse(SporeSpreadRules.canSpread(true, 0, 48, false, 1));
        assertTrue(SporeSpreadRules.canSpread(true, 0, 48, true, 2));
        assertFalse(SporeSpreadRules.canSpread(false, 0, 48, true, 0));
        int hitsPlain = 0;
        int hitsBloom = 0;
        for (int roll = 0; roll < SporeSpreadRules.ONE_IN; roll++) {
            hitsPlain += SporeSpreadRules.canSpread(true, 0, 48, false, roll) ? 1 : 0;
            hitsBloom += SporeSpreadRules.canSpread(true, 0, 48, true, roll) ? 1 : 0;
        }
        assertEquals(1, hitsPlain);
        assertEquals(3, hitsBloom);
    }

    @Test
    @DisplayName("shadow lichen: light <= 3, cap 32, never in a flare; withers 1 in 2 in a flare")
    void lichen() {
        assertTrue(LichenRules.canGrow(3, 31, 32, false));
        assertFalse(LichenRules.canGrow(4, 0, 32, false), "a torch stops it");
        assertFalse(LichenRules.canGrow(0, 32, 32, false));
        assertFalse(LichenRules.canGrow(0, 32, 64, false), "never raised");
        assertFalse(LichenRules.canGrow(0, 0, 32, true));
        assertTrue(LichenRules.canSpread(0, 0, 32, false, 0));
        assertFalse(LichenRules.canSpread(0, 0, 32, false, 1));
        assertTrue(LichenRules.withers(true, 0));
        assertFalse(LichenRules.withers(true, 1));
        assertFalse(LichenRules.withers(false, 0));
        assertEquals(10, LichenRules.ONE_IN);
    }

    @Test
    @DisplayName("xertz vine: down only, into air, length 12")
    void vine() {
        assertTrue(VineRules.canGrowDown(11, 12, true));
        assertFalse(VineRules.canGrowDown(12, 12, true));
        assertFalse(VineRules.canGrowDown(12, 20, true), "never raised");
        assertFalse(VineRules.canGrowDown(0, 12, false));
        assertEquals(10, VineRules.ONE_IN);
    }

    @Test
    @DisplayName("chunk count cache trusted for 200 ticks")
    void cache() {
        assertFalse(ChunkCounterRules.stale(100, 299));
        assertTrue(ChunkCounterRules.stale(100, 300));
        assertTrue(ChunkCounterRules.stale(-1, 0));
        assertTrue(ChunkCounterRules.stale(500, 100), "clock went backwards");
    }
}
