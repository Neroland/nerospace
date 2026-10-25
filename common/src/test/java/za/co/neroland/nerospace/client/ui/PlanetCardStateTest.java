package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.client.ui.PlanetCardData.State;

/** 004 §5: card state from here / gate / tier / selection, and the suit line. */
class PlanetCardStateTest {

    @Test
    void stateOrder() {
        assertEquals(State.HERE, PlanetCardData.stateFor(true, true, true, true));
        assertEquals(State.LOCKED_GATE, PlanetCardData.stateFor(false, true, true, true));
        assertEquals(State.LOCKED_TIER, PlanetCardData.stateFor(false, false, true, true));
        assertEquals(State.SELECTED, PlanetCardData.stateFor(false, false, false, true));
        assertEquals(State.AVAILABLE, PlanetCardData.stateFor(false, false, false, false));
    }

    @Test
    void suitLine() {
        assertEquals("gui.nerospace.card.suit_none", PlanetCardData.suitKey(false, Hazard.NONE, false));
        assertEquals("gui.nerospace.card.suit_t1", PlanetCardData.suitKey(true, Hazard.NONE, false));
        assertEquals("gui.nerospace.card.suit_thermal", PlanetCardData.suitKey(true, Hazard.HEAT, false));
        assertEquals("gui.nerospace.card.suit_cryo", PlanetCardData.suitKey(true, Hazard.COLD, false));
        assertEquals("gui.nerospace.card.suit_rad", PlanetCardData.suitKey(true, Hazard.COLD, true));
        assertEquals("gui.nerospace.card.suit_rad", PlanetCardData.suitKey(true, Hazard.RADIATION, false));
    }
}
