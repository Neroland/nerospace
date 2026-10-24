package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.5 Glowcap. */
@SuppressWarnings("unused")
class GrowthRulesTest {

    @Test
    @DisplayName("giant needs 5 air and soil; 40 % bonemeal")
    void giant() {
        assertTrue(GrowthRules.giantFits(5, true));
        assertFalse(GrowthRules.giantFits(4, true));
        assertFalse(GrowthRules.giantFits(9, false));
        assertEquals(0.4F, GrowthRules.BONEMEAL_CHANCE);
    }
}
