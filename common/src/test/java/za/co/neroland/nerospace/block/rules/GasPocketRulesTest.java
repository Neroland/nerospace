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

/** Helium-3 Pocket. */
@SuppressWarnings("unused")
class GasPocketRulesTest {

    @Test
    @DisplayName("silk or adjacent tank keeps the yield; otherwise half, min 1, and it leaks")
    void yield() {
        assertEquals(2, GasPocketRules.yield(true, false, 2));
        assertEquals(2, GasPocketRules.yield(false, true, 2));
        assertEquals(1, GasPocketRules.yield(false, false, 2));
        assertEquals(1, GasPocketRules.yield(false, false, 1), "min 1");
        assertEquals(2, GasPocketRules.yield(false, false, 5), "rounded down");
        assertTrue(GasPocketRules.leaks(false, false));
        assertFalse(GasPocketRules.leaks(true, false));
        assertEquals(20, GasPocketRules.LEAK_PARTICLES);
    }
}
