package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.7 Hull Window frost, Truss runs, Solar Wing, Docking Ring, Hydroponics; 005 §7.2/§7.4. */
@SuppressWarnings("unused")
class StationRulesTest {

    @Test
    @DisplayName("frost only in the station facing the void")
    void frost() {
        assertTrue(FrostRules.frosted(true, new boolean[] {false, true, false, false}));
        assertFalse(FrostRules.frosted(true, new boolean[4]));
        assertFalse(FrostRules.frosted(false, new boolean[] {true, true, true, true}));
    }

    @Test
    @DisplayName("truss run: up to 8, items-limited, stops at first non-air")
    void truss() {
        boolean[] open = {true, true, true, true, true, true, true, true, true, true};
        assertEquals(8, RunPlacementRules.runLength(64, open, 8));
        assertEquals(8, RunPlacementRules.runLength(64, open, 20), "never above 8");
        assertEquals(3, RunPlacementRules.runLength(3, open, 8));
        assertEquals(2, RunPlacementRules.runLength(64, new boolean[] {true, true, false, true}, 8));
        assertEquals(0, RunPlacementRules.runLength(64, new boolean[] {false}, 8));
        assertEquals(0, RunPlacementRules.runLength(0, open, 8));
    }

    @Test
    @DisplayName("solar wing: 2x T2 peak, class multiplier, exposure and enable gates")
    void wing() {
        assertEquals(200, SolarWingRules.output(100, true));
        assertEquals(0, SolarWingRules.output(100, false));
        assertEquals(300, SolarWingRules.output(100, true, 1.5, true));
        assertEquals(0, SolarWingRules.output(100, true, 1.5, false));
        assertEquals(0, SolarWingRules.output(100, true, Double.NaN, true));
        assertEquals(1.0, SolarReflectRules.reflectMultiplier(9, true), "never stacks with anorthosite");
    }

    @Test
    @DisplayName("dock position and 3x3 parts")
    void dock() {
        assertArrayEquals(new double[] {10.5, 65.25, -3.5}, DockRules.dockPosition(10, 64, -4, 1.25), 1.0e-9);
        assertEquals(4, DockRules.partIndex(0, 0));
        assertEquals(0, DockRules.partIndex(-1, -1));
        assertEquals(8, DockRules.partIndex(1, 1));
        assertEquals(-1, DockRules.partIndex(2, 0));
        assertTrue(DockRules.dropsSelf(4));
        assertFalse(DockRules.dropsSelf(0));
    }

    @Test
    @DisplayName("hydroponics: window within 3 above AND oxygen; class multiplier as tick skip")
    void hydro() {
        assertTrue(HydroRules.canGrow(true, true));
        assertFalse(HydroRules.canGrow(true, false));
        assertFalse(HydroRules.canGrow(false, true));
        assertTrue(HydroRules.classAllows(1.0, 0.99));
        assertTrue(HydroRules.classAllows(0.5, 0.49));
        assertFalse(HydroRules.classAllows(0.5, 0.5));
        assertEquals(3, HydroRules.WINDOW_RANGE);
    }
}
