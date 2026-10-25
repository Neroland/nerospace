package za.co.neroland.nerospace.world.planet.glacira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.block.rules.CrystalStageRules;

class GlaciraNumbersTest {

    @Test
    void clusterOutlineGrowsEveryStageAndStaysInsideTheBlock() {
        for (int s = 1; s <= CrystalStageRules.MAX_STAGE; s++) {
            assertTrue(GlaciraNumbers.clusterInset(s) < GlaciraNumbers.clusterInset(s - 1), "inset " + s);
            assertTrue(GlaciraNumbers.clusterHeight(s) > GlaciraNumbers.clusterHeight(s - 1), "height " + s);
        }
        for (int s = -1; s <= CrystalStageRules.MAX_STAGE + 1; s++) {
            assertTrue(GlaciraNumbers.clusterInset(s) >= 0 && GlaciraNumbers.clusterInset(s) < 8);
            assertTrue(GlaciraNumbers.clusterHeight(s) > 0 && GlaciraNumbers.clusterHeight(s) <= 16);
        }
    }

    @Test
    void freezeOnlyExposedSourcesUnderTheCap() {
        assertTrue(GlaciraNumbers.freezes(true, true, 0, 32));
        assertFalse(GlaciraNumbers.freezes(false, true, 0, 32), "not water");
        assertFalse(GlaciraNumbers.freezes(true, false, 0, 32), "roofed");
        assertFalse(GlaciraNumbers.freezes(true, true, 32, 32), "cap reached");
        assertFalse(GlaciraNumbers.freezes(true, true, 32, 1000), "config can never raise the cap above 32");
        assertFalse(GlaciraNumbers.freezes(true, true, 4, 4), "config may lower it");
    }

    @Test
    void cadenceMatchesTheSpec() {
        assertEquals(100, GlaciraNumbers.FREEZE_INTERVAL_TICKS);
        assertEquals(32, GlaciraNumbers.FREEZE_CAP_PER_EVENT);
    }
}
