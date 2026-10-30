package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Stripped Bio-Bark Log. */
@SuppressWarnings("unused")
class HealBedRulesTest {

    @Test
    @DisplayName("4 HP flat with ≥ 1 log within 4")
    void heal() {
        assertEquals(4.0F, HealBedRules.healOnWake(1));
        assertEquals(4.0F, HealBedRules.healOnWake(9), "flat, not per log");
        assertEquals(0.0F, HealBedRules.healOnWake(0));
        assertEquals(200, HealBedRules.REGEN_TICKS);
        assertEquals(4, HealBedRules.RADIUS);
    }
}
