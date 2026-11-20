package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.europa.block.EuropaHeat;

/** The Europa "liquid" test is water or brine only: lava and other fluids do not wet a seam or wake a vent. */
class EuropaHeatTest {

    @Test
    @DisplayName("water and brine are liquid; any other fluid (lava) or none is not")
    void liquidIsWaterOrBrine() {
        assertTrue(EuropaHeat.liquid(true, false));
        assertTrue(EuropaHeat.liquid(false, true));
        // Lava: a fluid that is not water, and not brine.
        assertFalse(EuropaHeat.liquid(false, false));
    }
}
