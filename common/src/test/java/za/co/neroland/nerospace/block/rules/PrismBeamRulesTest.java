package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Nyx Prism. */
@SuppressWarnings("unused")
class PrismBeamRulesTest {

    @Test
    @DisplayName("the beam is the 6 cells toward +X; inside it a flare adds +2; light 6 / 12")
    void beam() {
        assertFalse(PrismBeamRules.inBeam(0, 0, 0));
        assertTrue(PrismBeamRules.inBeam(1, 0, 0));
        assertTrue(PrismBeamRules.inBeam(6, 0, 0));
        assertFalse(PrismBeamRules.inBeam(7, 0, 0));
        assertFalse(PrismBeamRules.inBeam(-1, 0, 0), "never toward the sun");
        assertFalse(PrismBeamRules.inBeam(3, 1, 0));
        assertFalse(PrismBeamRules.inBeam(3, 0, 1));
        assertEquals(0, PrismBeamRules.flareRadiation(false, true));
        assertEquals(1, PrismBeamRules.flareRadiation(true, false));
        assertEquals(2, PrismBeamRules.flareRadiation(true, true));
        assertEquals(6, PrismBeamRules.light(false));
        assertEquals(12, PrismBeamRules.light(true));
    }
}
