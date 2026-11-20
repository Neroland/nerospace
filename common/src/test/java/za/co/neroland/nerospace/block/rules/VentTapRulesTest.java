package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Cryovent tap. */
class VentTapRulesTest {

    @Test
    @DisplayName("only a charged vent and an empty bucket tap")
    void canTap() {
        assertTrue(VentTapRules.canTap(true, true));
        assertFalse(VentTapRules.canTap(false, true));
        assertFalse(VentTapRules.canTap(true, false));
        assertFalse(VentTapRules.canTap(false, false));
    }

    @Test
    @DisplayName("one bucket, charge spent, back after 600 ticks")
    void recharge() {
        assertEquals(1, VentTapRules.BUCKETS_PER_TAP);
        assertFalse(VentTapRules.chargedAfterTap());
        assertFalse(VentTapRules.canTap(VentTapRules.chargedAfterTap(), true));
        assertEquals(600, VentTapRules.RECHARGE_TICKS);
        assertTrue(VentTapRules.recharges(false));
        assertFalse(VentTapRules.recharges(true));
    }
}
