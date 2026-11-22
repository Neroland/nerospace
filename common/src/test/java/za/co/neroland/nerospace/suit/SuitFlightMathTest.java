package za.co.neroland.nerospace.suit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.api.TemperatureBand;
import za.co.neroland.nerospace.suit.SuitFlightMath.Input;
import za.co.neroland.nerospace.suit.SuitFlightMath.Mode;

class SuitFlightMathTest {

    private static final Input STILL = new Input(false, false, false, false, false, true, false, true);

    @Test
    @DisplayName("gravity floor, heat by temperature, speed clamp, zero-G handover")
    void maths() {
        assertEquals(SuitEnergyMath.flightCost(60, 0.25, 1.0), SuitEnergyMath.flightCost(60, 0.1, 1.0),
                "below the floor costs the floor");
        assertTrue(SuitEnergyMath.flightCost(60, 0.38, 1.0) < SuitEnergyMath.flightCost(60, 1.0, 1.0));
        assertTrue(SuitFlightMath.heatGain(TemperatureBand.INFERNAL) > SuitFlightMath.heatGain(TemperatureBand.FRIGID));
        assertTrue(SuitFlightMath.heatLoss(TemperatureBand.FRIGID) > SuitFlightMath.heatLoss(TemperatureBand.INFERNAL));
        var cruise = SuitFlightMath.step(new Input(false, false, false, false, true, true, false, true), Mode.CRUISE,
                3, 0, 1.0, 0, 0, 60, 0.6, true, TemperatureBand.TEMPERATE);
        assertEquals(0.6, cruise.vx(), 1.0E-6);
        assertTrue(SuitFlightMath.zeroG(0.04));
        var eva = SuitFlightMath.step(STILL, Mode.CRUISE, 1, 0, 0.01, 0, 0, 60, 0.6, true, TemperatureBand.TEMPERATE);
        assertEquals(Mode.EVA_HANDOVER, eva.mode());
        assertEquals(0, eva.vx(), 0);
        var locked = SuitFlightMath.step(new Input(false, false, false, false, true, true, true, true), Mode.CRUISE,
                1, 0, 1, 0, 0, 60, 0.6, true, TemperatureBand.TEMPERATE);
        assertEquals(Mode.FAILSAFE, locked.mode());
        assertTrue(locked.descending());
    }
}
