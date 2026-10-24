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

/** 002 §1.1 Anorthosite, §10.10 no stacking with Solar Wing. */
@SuppressWarnings("unused")
class SolarReflectRulesTest {

    @Test
    @DisplayName("+25 % only for a full 3x3; never for a wing")
    void reflect() {
        assertEquals(1.25, SolarReflectRules.reflectMultiplier(9));
        assertEquals(1.0, SolarReflectRules.reflectMultiplier(8));
        assertEquals(1.0, SolarReflectRules.reflectMultiplier(0));
        assertEquals(1.25, SolarReflectRules.reflectMultiplier(20), "max +25 % regardless of count");
        assertEquals(1.0, SolarReflectRules.reflectMultiplier(9, true));
        assertEquals(40, SolarReflectRules.CHECK_INTERVAL_TICKS);
    }
}
