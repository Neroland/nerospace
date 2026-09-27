package za.co.neroland.nerospace.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.api.route.RouteQuote;

/** The cargo cost model (docs/CARGO-ROCKETS.md §4) — pure functions, no server. */
class CargoFormulasTest {

    @Test
    @DisplayName("travel time: base + cross-dimension surcharge + gravity term, clamped")
    void travelTicks() {
        assertEquals(CargoFormulas.TIME_BASE + CargoFormulas.TIME_PER_G, CargoFormulas.rawTravelTicks(1.0, 1.0, true));
        assertEquals(CargoFormulas.TIME_BASE + CargoFormulas.TIME_CROSS_DIM + CargoFormulas.TIME_PER_G,
                CargoFormulas.rawTravelTicks(1.0, 1.0, false));
        // Lighter worlds are quicker; heavier ones slower.
        assertTrue(CargoFormulas.rawTravelTicks(0.2, 0.2, false) < CargoFormulas.rawTravelTicks(1.0, 1.0, false));
        assertTrue(CargoFormulas.rawTravelTicks(2.0, 2.0, false) > CargoFormulas.rawTravelTicks(1.0, 1.0, false));
        // Clamped and NaN-safe.
        assertTrue(CargoFormulas.rawTravelTicks(10.0, 10.0, false) <= CargoFormulas.MAX_TRAVEL);
        assertTrue(CargoFormulas.rawTravelTicks(Double.NaN, 1.0, true) >= CargoFormulas.MIN_TRAVEL);
        assertTrue(CargoFormulas.rawTravelTicks(-5.0, -5.0, true) >= CargoFormulas.MIN_TRAVEL);
    }

    @Test
    @DisplayName("fuel: base + per-mass-unit, scaled by origin gravity with a floor")
    void fuel() {
        assertEquals(CargoFormulas.FUEL_BASE, CargoFormulas.rawFuelMb(0, 1.0));
        assertEquals(CargoFormulas.FUEL_BASE + 5 * CargoFormulas.FUEL_PER_UNIT, CargoFormulas.rawFuelMb(5, 1.0));
        assertEquals(Math.round((CargoFormulas.FUEL_BASE + 3 * CargoFormulas.FUEL_PER_UNIT) * 0.5), CargoFormulas.rawFuelMb(3, 0.5));
        // Weightless origins still cost the floor; negative mass is treated as zero.
        assertEquals(Math.round(CargoFormulas.FUEL_BASE * CargoFormulas.MIN_GRAVITY), CargoFormulas.rawFuelMb(0, 0.0));
        assertEquals(CargoFormulas.rawFuelMb(0, 1.0), CargoFormulas.rawFuelMb(-7, 1.0));
        assertTrue(CargoFormulas.rawFuelMb(0, 1.0) >= 1);
    }

    @Test
    @DisplayName("mass units: an empty manifest weighs nothing")
    void massOfNothing() {
        assertEquals(0, CargoFormulas.massUnits(List.of()));
        assertEquals(0, CargoFormulas.massUnits(null));
    }

    @Test
    @DisplayName("quote: return_empty adds an empty leg priced from the destination's gravity")
    void quoteReturnLeg() {
        RouteQuote oneWay = CargoFormulas.quote(1.0, 0.4, false, List.of(), false);
        RouteQuote roundTrip = CargoFormulas.quote(1.0, 0.4, false, List.of(), true);
        assertEquals(oneWay.travelTicks(), roundTrip.travelTicks(), "the return leg does not lengthen the outbound flight");
        assertEquals(oneWay.fuelMb() + CargoFormulas.fuelMb(0, 0.4), roundTrip.fuelMb());
        assertTrue(oneWay.crossDimension());
        assertFalse(CargoFormulas.quote(1.0, 1.0, true, List.of(), false).crossDimension());
        assertEquals(0, oneWay.massUnits());
        assertEquals((oneWay.travelTicks() + 19) / 20, oneWay.travelSeconds());
    }

    @Test
    @DisplayName("config-scaled variants default to the raw values (multiplier 1.0)")
    void scaledDefaults() {
        assertEquals(CargoFormulas.rawTravelTicks(1.0, 1.0, false), CargoFormulas.travelTicks(1.0, 1.0, false));
        assertEquals(CargoFormulas.rawFuelMb(4, 1.0), CargoFormulas.fuelMb(4, 1.0));
    }
}
