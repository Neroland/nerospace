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
}
