package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Void Crystal (3D data indices: 0 down, 1 up, 2 N, 3 S, 4 W, 5 E). */
@SuppressWarnings("unused")
class RefractRulesTest {

    @Test
    @DisplayName("turns 90° toward the facing; a beam along the facing axis passes")
    void refract() {
        assertEquals(5, RefractRules.outDirection(2, 5), "north beam, east facing → east");
        assertEquals(1, RefractRules.outDirection(4, 1), "west beam, up facing → up");
        assertEquals(5, RefractRules.outDirection(5, 5), "parallel passes");
        assertEquals(4, RefractRules.outDirection(4, 5), "anti-parallel passes");
        assertEquals(7, RefractRules.outDirection(7, 1), "invalid input untouched");
        for (int in = 0; in < 6; in++) {
            for (int f = 0; f < 6; f++) {
                int out = RefractRules.outDirection(in, f);
                assertTrue(out == in || out == f);
            }
        }
    }
}
