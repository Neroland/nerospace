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
    @DisplayName("an oxidant crystal gives 400 mB and no ingot, and is refined only with 400 mB of buffer free")
    void oxidantCrystal() {
        assertEquals(400, RefineryRules.OXIDANT_OXYGEN_MB);
        assertEquals(400, RefineryRules.oxygenFor(true));
        assertEquals(0, RefineryRules.titaniumFor(true));
        assertTrue(RefineryRules.canRefineOxidant(400));
        assertTrue(RefineryRules.canRefineOxidant(FuelRefineryBlockEntity.OXYGEN_CAPACITY));
        assertFalse(RefineryRules.canRefineOxidant(399));
        assertFalse(RefineryRules.canRefineOxidant(0));
        // Two crystals fit the buffer; the third waits for the gas to be drawn off rather than venting.
        long free = FuelRefineryBlockEntity.OXYGEN_CAPACITY;
        int refined = 0;
        while (RefineryRules.canRefineOxidant(free)) {
            free -= RefineryRules.oxygenFor(true);
            refined++;
        }
        assertEquals(2, refined);
        // The ilmenite line is what it was: 250 mB and one ingot, whatever the buffer holds.
        assertEquals(250, RefineryRules.ILMENITE_OXYGEN_MB);
        assertEquals(250, RefineryRules.oxygenFor(false));
        assertEquals(1, RefineryRules.titaniumFor(false));
        assertEquals(1, RefineryRules.TITANIUM_PER_ILMENITE);
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
