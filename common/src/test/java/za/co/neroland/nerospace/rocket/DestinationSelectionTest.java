package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.rocket.DestinationSelection.Outcome;

/** The destination click → index/reason rules the screens predict with and the server guard decides with. */
class DestinationSelectionTest {

    private static int bits(int... indices) {
        int mask = 0;
        for (int i : indices) {
            mask |= 1 << i;
        }
        return mask;
    }

    private static final int T1_OVERWORLD = bits(Destinations.STATION, Destinations.LUNA);
    private static final int T4_OVERWORLD = bits(Destinations.STATION, Destinations.GREENXERTZ, Destinations.CINDARA,
            Destinations.GLACIRA, Destinations.NYXARA);
    private static final int NYXARA_GATE = bits(Destinations.NYXARA);

    @Test
    @DisplayName("server guard: a reachable world is accepted, the current one is a no-op")
    void acceptsReachable() {
        assertEquals(Outcome.OK, DestinationSelection.evaluate(Destinations.LUNA, T1_OVERWORLD, 0,
                Destinations.STATION, false));
        assertEquals(Outcome.ALREADY_SELECTED, DestinationSelection.evaluate(Destinations.STATION, T1_OVERWORLD, 0,
                Destinations.STATION, false));
        assertTrue(Outcome.ALREADY_SELECTED.accepted());
        assertNull(Outcome.OK.langKey());
    }

    @Test
    @DisplayName("server guard: launching, out of range, gate-locked and bad ids are refused with a reason")
    void refusesWithReason() {
        assertEquals(Outcome.LAUNCHING, DestinationSelection.evaluate(Destinations.LUNA, T1_OVERWORLD, 0,
                Destinations.STATION, true));
        assertEquals(Outcome.OUT_OF_RANGE, DestinationSelection.evaluate(Destinations.MARS, T1_OVERWORLD, 0,
                Destinations.STATION, false));
        assertEquals(Outcome.GATE_LOCKED, DestinationSelection.evaluate(Destinations.NYXARA, T4_OVERWORLD,
                NYXARA_GATE, Destinations.GLACIRA, false));
        assertEquals(Outcome.OUT_OF_RANGE, DestinationSelection.evaluate(-1, T1_OVERWORLD, 0, 1, false));
        assertEquals(Outcome.OUT_OF_RANGE, DestinationSelection.evaluate(31, -1, 0, 1, false));
        for (Outcome o : Outcome.values()) {
            assertEquals(o.accepted(), o.langKey() == null, o + " must name a reason exactly when refused");
        }
    }

    @Test
    @DisplayName("card rule: Observatory previews and the you-are-here card explain themselves")
    void cardReasons() {
        assertEquals(Outcome.PREVIEW, DestinationSelection.evaluateCard(Destinations.MARS, T1_OVERWORLD, 0,
                Destinations.STATION, false, true));
        assertEquals(Outcome.HERE, DestinationSelection.evaluateCard(Destinations.HOME, T1_OVERWORLD, 0,
                Destinations.STATION, true, false));
        // Same-dimension hop: the current world is in the mask when it has pads, so it is selectable.
        assertEquals(Outcome.OK, DestinationSelection.evaluateCard(Destinations.HOME,
                T1_OVERWORLD | bits(Destinations.HOME), 0, Destinations.STATION, true, false));
        assertEquals(Outcome.OK, DestinationSelection.evaluateCard(Destinations.LUNA, T1_OVERWORLD, 0,
                Destinations.STATION, false, false));
    }

    @Test
    @DisplayName("stepping wraps, goes both ways and skips gate-locked worlds")
    void stepping() {
        int n = Destinations.all().size();
        assertEquals(Destinations.LUNA, DestinationSelection.step(T1_OVERWORLD, 0, Destinations.STATION, 1, n));
        assertEquals(Destinations.STATION, DestinationSelection.step(T1_OVERWORLD, 0, Destinations.LUNA, 1, n));
        assertEquals(Destinations.LUNA, DestinationSelection.step(T1_OVERWORLD, 0, Destinations.STATION, -1, n));
        assertEquals(Destinations.STATION,
                DestinationSelection.step(T4_OVERWORLD, NYXARA_GATE, Destinations.GLACIRA, 1, n));
        assertEquals(Destinations.NYXARA, DestinationSelection.step(T4_OVERWORLD, 0, Destinations.GLACIRA, 1, n));
        assertEquals(Destinations.STATION,
                DestinationSelection.step(bits(Destinations.STATION), 0, Destinations.STATION, 1, n));
        assertFalse(DestinationSelection.selectable(Destinations.NYXARA, T4_OVERWORLD, NYXARA_GATE));
    }
}
