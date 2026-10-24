package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.1 Lunar Basalt sinter. */
@SuppressWarnings("unused")
class SolarSinterRulesTest {

    @Test
    @DisplayName("only placed basalt, sky-exposed, daylight >= 0.5, on Luna; 1 in 6")
    void sinter() {
        assertTrue(SolarSinterRules.canSinter(true, true, 0.5, true));
        assertFalse(SolarSinterRules.canSinter(false, true, 1.0, true), "worldgen maria never sinter");
        assertFalse(SolarSinterRules.canSinter(true, false, 1.0, true));
        assertFalse(SolarSinterRules.canSinter(true, true, 0.49, true));
        assertFalse(SolarSinterRules.canSinter(true, true, 1.0, false));
        assertEquals(6, SolarSinterRules.ONE_IN);
        assertTrue(SolarSinterRules.rolled(0));
        assertFalse(SolarSinterRules.rolled(5));
    }
}
