package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.client.ui.PendingSelection;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.DestinationSelection.Outcome;

/**
 * Regression for "picked Nyxara, landed on Glacira, screen said the rocket kept its course": the per-viewer gate
 * mask must reach the screen, a creative player bypasses Core gates, and a refusal names the server's reason.
 */
class DestinationGateSyncTest {

    private static final int NYXARA_GATE = 1 << Destinations.NYXARA;
    private static final int T4_FROM_OVERWORLD = Destinations.availableMask(RocketTier.TIER_4, ModDimensions.OVERWORLD);
    private static final int T4_DEFAULT = Destinations.defaultIndex(RocketTier.TIER_4, ModDimensions.OVERWORLD);

    @Test
    @DisplayName("viewer cache: a never-computed value is due (the old guard overflowed and read as fresh forever)")
    @SuppressWarnings("ConstantValue")
    void neverComputedIsDue() {
        long now = 1234L;
        // The old guard: now - Long.MIN_VALUE overflows negative, so a never-computed gate mask "stayed fresh".
        boolean oldGuardSkipped = now - Long.MIN_VALUE < RocketEntity.VIEWER_DATA_REFRESH_TICKS && now >= Long.MIN_VALUE;
        assertTrue(oldGuardSkipped, "documents the overflow that kept the synced gate mask at 0");

        assertTrue(DestinationSelection.refreshDue(now, DestinationSelection.NEVER, 20));
        assertTrue(DestinationSelection.refreshDue(0L, DestinationSelection.NEVER, 20));
        assertTrue(DestinationSelection.refreshDue(Long.MAX_VALUE, DestinationSelection.NEVER, 20));
        assertFalse(DestinationSelection.refreshDue(now, now - 19, 20));
        assertTrue(DestinationSelection.refreshDue(now, now - 20, 20));
        assertTrue(DestinationSelection.refreshDue(now, now + 5, 20), "a clock that went backwards recomputes");
    }

    @Test
    @DisplayName("reported case: Tier 4 on the overworld, survival, no deep-space clearance")
    void ownerReportSurvivalWithoutClearance() {
        assertEquals(Destinations.GLACIRA, T4_DEFAULT);
        assertTrue((T4_FROM_OVERWORLD & NYXARA_GATE) != 0, "Nyxara is in Tier 4 range");

        // Before the fix the screen saw gate mask 0: Nyxara looked selectable ...
        assertEquals(Outcome.OK, DestinationSelection.evaluateCard(Destinations.NYXARA, T4_FROM_OVERWORLD, 0,
                T4_DEFAULT, false, false));
        // ... the server refused it, the selection stayed Glacira, and the launch flew there.
        assertEquals(Outcome.GATE_LOCKED, DestinationSelection.evaluate(Destinations.NYXARA, T4_FROM_OVERWORLD,
                NYXARA_GATE, T4_DEFAULT, false));

        // With the real per-viewer mask the card is not offered and names the gate.
        int viewerMask = DestinationSelection.viewerGateMask(false, NYXARA_GATE);
        assertFalse(DestinationSelection.selectable(Destinations.NYXARA, T4_FROM_OVERWORLD, viewerMask));
        Outcome card = DestinationSelection.evaluateCard(Destinations.NYXARA, T4_FROM_OVERWORLD, viewerMask,
                T4_DEFAULT, false, false);
        assertEquals(Outcome.GATE_LOCKED, card);
        assertEquals("gui.nerospace.dest.reason.gate", DestinationSelection.refusalKey(card));
        assertFalse(DestinationSelection.gateOpenFor(false, false), "launch re-check refuses too");
    }

    @Test
    @DisplayName("a server refusal reaches the screen with its reason, never the generic one")
    void refusalCarriesServerReason() {
        PendingSelection p = new PendingSelection();
        int before = DestinationSelection.NO_OUTCOME;
        p.begin(Destinations.NYXARA, T4_DEFAULT, before, 0L);
        int answer = DestinationSelection.encodeOutcome(1, Outcome.GATE_LOCKED);
        assertEquals(PendingSelection.Result.REFUSED, p.reconcile(T4_DEFAULT, answer, 40L));
        assertEquals(Outcome.GATE_LOCKED, p.refusal());
        assertEquals("gui.nerospace.dest.reason.gate", DestinationSelection.refusalKey(p.refusal()));
        assertEquals(T4_DEFAULT, p.shown(T4_DEFAULT));
    }

    @Test
    @DisplayName("creative bypasses Core gates for selection, stepping and launch")
    void creativeBypassesGates() {
        int viewerMask = DestinationSelection.viewerGateMask(true, NYXARA_GATE);
        assertEquals(0, viewerMask);
        assertTrue(DestinationSelection.selectable(Destinations.NYXARA, T4_FROM_OVERWORLD, viewerMask));
        assertEquals(Outcome.OK, DestinationSelection.evaluate(Destinations.NYXARA, T4_FROM_OVERWORLD, viewerMask,
                T4_DEFAULT, false));
        assertEquals(Destinations.NYXARA, DestinationSelection.step(T4_FROM_OVERWORLD, viewerMask, Destinations.MARS,
                1, Destinations.all().size()));
        assertTrue(DestinationSelection.gateOpenFor(true, false));
    }

    @Test
    @DisplayName("survival and spectators are held to the gate; clearance opens it")
    void survivalHeldToGate() {
        int viewerMask = DestinationSelection.viewerGateMask(false, NYXARA_GATE);
        assertEquals(NYXARA_GATE, viewerMask);
        assertEquals(Destinations.STATION, DestinationSelection.step(T4_FROM_OVERWORLD, viewerMask, Destinations.MARS,
                1, Destinations.all().size()), "stepping skips the gated world");
        assertEquals(Outcome.OK, DestinationSelection.evaluate(Destinations.NYXARA, T4_FROM_OVERWORLD,
                DestinationSelection.viewerGateMask(false, 0), T4_DEFAULT, false), "clearance granted");
        assertTrue(DestinationSelection.gateOpenFor(false, true));
    }

    @Test
    @DisplayName("outcome codes survive the 16-bit sync and always change between answers")
    void outcomeCodes() {
        assertNull(DestinationSelection.decodeOutcome(DestinationSelection.NO_OUTCOME));
        assertNull(DestinationSelection.decodeOutcome(-5));
        for (int seq = 0; seq < 5000; seq++) {
            for (Outcome o : Outcome.values()) {
                int code = DestinationSelection.encodeOutcome(seq, o);
                assertTrue(code > 0 && code < Short.MAX_VALUE, "code " + code);
                assertEquals(o, DestinationSelection.decodeOutcome(code));
                assertNotEquals(code, DestinationSelection.encodeOutcome(seq + 1, o));
            }
        }
        assertEquals("gui.nerospace.dest.reason.refused", DestinationSelection.refusalKey(null));
        assertEquals("gui.nerospace.dest.reason.only_one", DestinationSelection.refusalKey(Outcome.ALREADY_SELECTED));
    }
}
