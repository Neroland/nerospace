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

/** Hematite magnet. */
@SuppressWarnings("unused")
class ItemMagnetRulesTest {

    @Test
    @DisplayName("0.2 b/t toward the breaker within 3 blocks; null outside")
    void pull() {
        double[] v = ItemMagnetRules.pull(2.0, 0.0, 0.0, 3.0, 0.2);
        assertArrayEquals(new double[] {0.2, 0.0, 0.0}, v, 1.0e-9);
        double[] diag = ItemMagnetRules.pull(1.0, 1.0, 1.0, 3.0, 0.2);
        assertEquals(0.2, Math.sqrt(diag[0] * diag[0] + diag[1] * diag[1] + diag[2] * diag[2]), 1.0e-9);
        assertNull(ItemMagnetRules.pull(3.01, 0.0, 0.0, 3.0, 0.2));
        assertNull(ItemMagnetRules.pull(0.0, 0.0, 0.0, 3.0, 0.2), "already there");
        assertNull(ItemMagnetRules.pull(Double.NaN, 0.0, 0.0, 3.0, 0.2));
    }

    @Test
    @DisplayName("only with iron+ tools, for 20 ticks")
    void active() {
        assertTrue(ItemMagnetRules.active(true, 20));
        assertFalse(ItemMagnetRules.active(true, 21));
        assertFalse(ItemMagnetRules.active(false, 0));
    }
}
