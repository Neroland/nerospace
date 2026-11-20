package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lineae Salt seep. */
class SeepRulesTest {

    @Test
    @DisplayName("seeps for a cavity 1 to 12 below, not at 0 or past 12")
    void seeps() {
        assertFalse(SeepRules.seeps(0));
        assertTrue(SeepRules.seeps(1));
        assertTrue(SeepRules.seeps(12));
        assertFalse(SeepRules.seeps(13));
        assertFalse(SeepRules.seeps(-1));
    }

    @Test
    @DisplayName("the column scan stops at 12 cells")
    void depth() {
        assertEquals(0, SeepRules.depthToCavity(new boolean[12]));
        boolean[] third = new boolean[12];
        third[2] = true;
        third[7] = true;
        assertEquals(3, SeepRules.depthToCavity(third));
        boolean[] far = new boolean[20];
        far[12] = true;
        assertEquals(0, SeepRules.depthToCavity(far));
        far[11] = true;
        assertEquals(12, SeepRules.depthToCavity(far));
    }

    @Test
    @DisplayName("one scan in forty calls, one particle at most")
    void gate() {
        int scans = 0;
        for (int roll = 0; roll < SeepRules.GATE; roll++) {
            if (SeepRules.scans(roll)) {
                scans++;
            }
        }
        assertEquals(1, scans);
        assertEquals(40, SeepRules.GATE);
        assertTrue(SeepRules.PARTICLES_PER_CALL <= 1);
    }
}
