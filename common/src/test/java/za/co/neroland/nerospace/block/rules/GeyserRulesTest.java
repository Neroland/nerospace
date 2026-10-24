package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.4 Frozen Geyser. */
@SuppressWarnings("unused")
class GeyserRulesTest {

    @Test
    @DisplayName("+1.2 dy when charged, fall grace 60, every 160 ticks")
    void launch() {
        assertEquals(1.2, GeyserRules.launchVelocity(true, 0.0), 1.0e-9);
        assertEquals(1.1, GeyserRules.launchVelocity(true, -0.1), 1.0e-9);
        assertEquals(-0.1, GeyserRules.launchVelocity(false, -0.1), 1.0e-9);
        assertEquals(60, GeyserRules.FALL_GRACE_TICKS);
        assertEquals(160, GeyserRules.INTERVAL_TICKS);
    }
}
