package za.co.neroland.nerospace.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Fuel Refinery 2.0 maths: the Mars oxidiser boost and the Luna raw ilmenite line. */
class RefineryRulesTest {

    private static final int BATCH = FuelRefineryBlockEntity.MB_PER_BATCH;

    @Test
    @DisplayName("a boosted batch yields +15 %, an unboosted one the 1.x amount")
    void batchYield() {
        assertEquals(2_000, RefineryRules.batchMb(BATCH, false));
        assertEquals(2_300, RefineryRules.batchMb(BATCH, true));
    }

    @Test
    @DisplayName("one oxidiser covers four buckets of base output (two batches), then the next one is used")
    void oxidiserCoverage() {
        int coverage = 0;
        int oxidisers = 2;
        int produced = 0;
        int used = 0;
        for (int batch = 0; batch < 5; batch++) {
            boolean inSlot = oxidisers > 0;
            if (RefineryRules.consumesOxidiser(coverage, inSlot)) {
                oxidisers--;
                used++;
                coverage = RefineryRules.OXIDISER_COVERAGE_MB;
            }
            boolean boosted = coverage > 0;
            produced += RefineryRules.batchMb(BATCH, boosted);
            if (boosted) {
                coverage = RefineryRules.coverageAfterBatch(coverage, BATCH);
            }
        }
        assertEquals(2, used);
        // Four boosted batches (2 oxidisers x 2 batches) and one plain one.
        assertEquals(4 * 2_300 + 2_000, produced);
        assertEquals(0, coverage);
    }

    @Test
    @DisplayName("the tank-space check sees the boost before the oxidiser is consumed")
    void boostedLookahead() {
        assertTrue(RefineryRules.boosted(0, true));
        assertTrue(RefineryRules.boosted(2_000, false));
        assertFalse(RefineryRules.boosted(0, false));
        assertFalse(RefineryRules.consumesOxidiser(2_000, true));
        assertFalse(RefineryRules.consumesOxidiser(0, false));
    }

    @Test
    @DisplayName("raw ilmenite gives one titanium ingot plus oxygen, and only when the output slot has room")
    void ilmenite() {
        assertEquals(1, RefineryRules.TITANIUM_PER_ILMENITE);
        assertTrue(RefineryRules.ILMENITE_OXYGEN_MB > 0);
        assertTrue(RefineryRules.titaniumFits(true, false, 0, 64));
        assertTrue(RefineryRules.titaniumFits(false, true, 63, 64));
        assertFalse(RefineryRules.titaniumFits(false, true, 64, 64));
        assertFalse(RefineryRules.titaniumFits(false, false, 1, 64));
    }

    @Test
    @DisplayName("the 2.0 slots are appended after the 1.x slots")
    void slotLayout() {
        assertEquals(0, FuelRefineryBlockEntity.CARBON_SLOT);
        assertEquals(1, FuelRefineryBlockEntity.CATALYST_SLOT);
        assertEquals(2, FuelRefineryBlockEntity.OXIDISER_SLOT);
        assertEquals(3, FuelRefineryBlockEntity.ILMENITE_SLOT);
        assertEquals(4, FuelRefineryBlockEntity.TITANIUM_SLOT);
        assertEquals(5, FuelRefineryBlockEntity.SIZE);
    }
}
