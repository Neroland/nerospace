package za.co.neroland.nerospace.suit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SuitEnergyMathTest {

    @Test
    void batteriesAndCells() {
        assertEquals(50_000, SuitEnergyMath.capacity(SuitFrame.MK1, 0, 1.0));
        assertEquals(100_000, SuitEnergyMath.capacity(SuitFrame.MK2, 0, 1.0));
        assertEquals(250_000, SuitEnergyMath.capacity(SuitFrame.MK3, 0, 1.0));
        assertEquals(500_000, SuitEnergyMath.capacity(SuitFrame.MK4, 0, 1.0));
        assertEquals(1_000_000, SuitEnergyMath.capacity(SuitFrame.MK4, 2, 1.0));
    }

    @Test
    void reserveAndFreeEnergy() {
        assertEquals(15_000, SuitEnergyMath.reserve(500_000, 3));
        assertFalse(SuitEnergyMath.aboveReserve(15_000, 500_000, 3));
        assertTrue(SuitEnergyMath.aboveReserve(15_001, 500_000, 3));
        assertEquals(0, SuitEnergyMath.cost(40, 0));
    }
}
