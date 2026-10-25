package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.station.ObservatoryRules;

/** Telescope sightline reveal: dormant 0; research 1 + min(2, Void Crystals in the column). */
class ObservatoryRulesTest {

    @Test
    void reveal() {
        assertEquals(0, ObservatoryRules.revealCount(5, false));
        assertEquals(1, ObservatoryRules.revealCount(0, true));
        assertEquals(3, ObservatoryRules.revealCount(2, true));
        assertEquals(3, ObservatoryRules.revealCount(5, true));
        assertEquals(2, ObservatoryRules.revealCount(List.of(Boolean.FALSE, Boolean.TRUE, Boolean.FALSE), true));
    }
}
