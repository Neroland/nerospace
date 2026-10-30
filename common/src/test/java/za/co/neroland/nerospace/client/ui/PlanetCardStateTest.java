package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.client.ui.PlanetCardData.State;

/** Card state from here / gate / tier / selection, and the suit line. */
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

    @Test
    void suitLinePerPlanet() {
        java.util.Map<za.co.neroland.nerospace.world.planet.PlanetDefinition, String> expected = java.util.Map.of(
                za.co.neroland.nerospace.world.planet.Planets.STATION, "gui.nerospace.card.suit_t1",
                za.co.neroland.nerospace.world.planet.Planets.LUNA, "gui.nerospace.card.suit_t1",
                za.co.neroland.nerospace.world.planet.Planets.GREENXERTZ, "gui.nerospace.card.suit_t1",
                za.co.neroland.nerospace.world.planet.Planets.MARS, "gui.nerospace.card.suit_rad",
                za.co.neroland.nerospace.world.planet.Planets.CINDARA, "gui.nerospace.card.suit_thermal",
                za.co.neroland.nerospace.world.planet.Planets.GLACIRA, "gui.nerospace.card.suit_cryo",
                za.co.neroland.nerospace.world.planet.Planets.NYXARA, "gui.nerospace.card.suit_rad");
        expected.forEach((p, key) -> {
            za.co.neroland.nerospace.api.PlanetTraits t = p.traits();
            boolean rad = PlanetCardData.needsRadLining(t.radiation(), t.hazard(), p.weather().event());
            // The card always draws the suit line as for an airless world (Greenxertz' spores still want a suit).
            assertEquals(key, PlanetCardData.suitKey(true, t.hazard(), rad,
                    PlanetCardData.requiredSuit(t.airless(), t.hazard(), t.temperatureBand())), p.id().toString());
        });
        // Mars without its storms is a plain Tier 2 world: any Tier 2 class suit keeps its cold out.
        assertEquals("gui.nerospace.card.suit_t2", PlanetCardData.suitKey(true, Hazard.COLD, false,
                za.co.neroland.nerospace.world.OxygenManager.SuitTier.TIER_2));
    }
}
