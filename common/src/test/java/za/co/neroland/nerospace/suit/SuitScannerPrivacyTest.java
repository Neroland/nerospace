package za.co.neroland.nerospace.suit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SuitScannerPrivacyTest {

    @Test
    void worldgenOreOnly() {
        assertTrue(SuitScannerRules.visible(true, true, false, false));
        assertFalse(SuitScannerRules.visible(true, false, false, false), "not generated");
        assertFalse(SuitScannerRules.visible(true, true, true, false), "container");
        assertFalse(SuitScannerRules.visible(true, true, false, true), "player placed");
        assertFalse(SuitScannerRules.visible(false, true, false, false), "not an ore");
        assertEquals(32, SuitScannerRules.radius(32));
        assertEquals(8, SuitScannerRules.radius(1));
        assertEquals(64, SuitScannerRules.radius(1000));
        assertEquals(100, SuitScannerRules.cooldown(100));
    }
}
