package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Basalt Spire. */
@SuppressWarnings("unused")
class LightningRulesTest {

    @Test
    @DisplayName("a safe top cancels the strike; ember storms need intensity ≥ 0.7")
    void strike() {
        assertFalse(LightningRules.strikeAllowed(true));
        assertTrue(LightningRules.strikeAllowed(false));
        assertTrue(LightningRules.emberStorm(true, 0.7));
        assertFalse(LightningRules.emberStorm(true, 0.69));
        assertFalse(LightningRules.emberStorm(false, 1.0));
        assertEquals(200, LightningRules.INTERVAL_TICKS);
    }
}
