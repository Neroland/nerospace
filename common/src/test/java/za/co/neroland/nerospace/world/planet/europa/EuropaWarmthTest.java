package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.block.rules.VentWarmthRules;

/** The reach of Europa's cold shield: the scan sphere and the basalt radius. */
class EuropaWarmthTest {

    @Test
    @DisplayName("warm basalt shields within 2 blocks (squared distance 4), not beyond")
    void basaltReach() {
        assertEquals(2, EuropaWarmth.BASALT_RADIUS);
        assertTrue(EuropaWarmth.basaltReaches(0));
        assertTrue(EuropaWarmth.basaltReaches(4));
        assertFalse(EuropaWarmth.basaltReaches(5));
    }

    @Test
    @DisplayName("the scan covers the radius-5 sphere: 515 cells, and the basalt reach lies inside it")
    void sphere() {
        int r = (int) VentWarmthRules.RADIUS;
        int cells = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= r * r) {
                        cells++;
                    }
                }
            }
        }
        assertEquals(515, cells);
        assertTrue(EuropaWarmth.BASALT_RADIUS < r);
    }
}
