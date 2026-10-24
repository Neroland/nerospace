package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.4 Aurora Stone, §10.3. */
@SuppressWarnings("unused")
class AuroraRulesTest {

    @Test
    @DisplayName("lit below 0.25 daylight, every 200 ticks, light 8")
    void lit() {
        assertTrue(AuroraRules.lit(0.0));
        assertTrue(AuroraRules.lit(0.249));
        assertFalse(AuroraRules.lit(0.25));
        assertFalse(AuroraRules.lit(1.0));
        assertEquals(200, AuroraRules.INTERVAL_TICKS);
        assertEquals(8, AuroraRules.LIGHT);
    }
}
