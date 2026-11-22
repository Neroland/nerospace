package za.co.neroland.nerospace.suit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OxygenBudgetTest {

    @Test
    void legacySuitIsUnchanged() {
        assertEquals(900, SuitOxygenMath.capacity(true, 0, 1.0));
        assertEquals(300, SuitOxygenMath.capacity(false, 0, 1.0));
        assertEquals(3, SuitOxygenMath.drain(true, 0, false, 1.0));
        assertEquals(30, SuitOxygenMath.drain(false, 0, false, 1.0));
        assertEquals(12, SuitOxygenMath.drain(true, 0, true, 1.0));
    }

    @Test
    void tanksAndReclaimers() {
        assertEquals(1350, SuitOxygenMath.capacity(true, 1, 1.0));
        assertEquals(1800, SuitOxygenMath.capacity(true, 2, 1.0));
        assertEquals(1800, SuitOxygenMath.capacity(true, 9, 1.0));
        int one = SuitOxygenMath.drain(true, 1, false, 1.0);
        int two = SuitOxygenMath.drain(true, 2, false, 1.0);
        assertTrue(one < 3 && one >= 1);
        assertTrue(two <= one);
        // Floor at half of 3, then the shared scale, so two reclaimers cannot go under that floor.
        assertEquals(SuitOxygenMath.scale((int) Math.round(3 * 0.5), 1.0), two);
    }
}
