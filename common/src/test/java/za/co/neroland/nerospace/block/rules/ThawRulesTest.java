package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.4 Permafrost. */
@SuppressWarnings("unused")
class ThawRulesTest {

    @Test
    @DisplayName("hydration never thaws; terraform stage 3 does")
    void thaw() {
        assertFalse(ThawRules.canThaw(true, 3));
        assertFalse(ThawRules.canThaw(false, 2));
        assertTrue(ThawRules.canThaw(false, 3));
    }

    @Test
    @DisplayName("non-permafrost columns are unchanged by the terraformer thaw (1.6.0 chunks continue as before)")
    void nonPermafrostUnchanged() {
        assertFalse(ThawRules.hydrationSkipsFill(false), "ordinary basin floors still fill");
        for (int stage = 0; stage <= 3; stage++) {
            for (int d = -2; d <= 10; d++) {
                assertEquals(ThawRules.Thaw.NONE, ThawRules.thawAt(false, d, stage), "depth " + d + " stage " + stage);
            }
        }
    }

    @Test
    @DisplayName("permafrost: no meltwater at stage 2; grass over dirt, four deep, at stage 3")
    void permafrostThaw() {
        assertTrue(ThawRules.hydrationSkipsFill(true));
        assertEquals(ThawRules.Thaw.GRASS, ThawRules.thawAt(true, 1, 3));
        for (int d = 2; d <= ThawRules.THAW_DEPTH; d++) {
            assertEquals(ThawRules.Thaw.DIRT, ThawRules.thawAt(true, d, 3));
        }
        assertEquals(ThawRules.Thaw.NONE, ThawRules.thawAt(true, ThawRules.THAW_DEPTH + 1, 3));
        assertEquals(ThawRules.Thaw.NONE, ThawRules.thawAt(true, 1, 2), "never before stage 3");
    }
}
