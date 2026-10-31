package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.rocket.DestinationSelection;
import za.co.neroland.nerospace.rocket.DestinationSelection.Outcome;

/** The optimistic highlight: shown at once, settled by the synced (server) index. */
class PendingSelectionTest {

    @Test
    void confirmedWhenTheServerApplies() {
        PendingSelection p = new PendingSelection();
        p.begin(5, 1, 0L);
        assertEquals(5, p.shown(1));
        assertEquals(PendingSelection.Result.NONE, p.reconcile(1, 100L));
        assertEquals(PendingSelection.Result.CONFIRMED, p.reconcile(5, 150L));
        assertFalse(p.active());
        assertEquals(5, p.shown(5));
    }

    @Test
    void overriddenWhenTheServerPicksAnother() {
        PendingSelection p = new PendingSelection();
        p.begin(7, 4, 0L);
        assertEquals(PendingSelection.Result.OVERRIDDEN, p.reconcile(1, 50L));
        assertEquals(1, p.shown(1));
    }

    @Test
    void refusedWhenTheServerKeepsTheOldValue() {
        PendingSelection p = new PendingSelection();
        p.begin(5, 1, 0L);
        assertEquals(PendingSelection.Result.NONE, p.reconcile(1, PendingSelection.TIMEOUT_MS - 1));
        assertEquals(PendingSelection.Result.REFUSED, p.reconcile(1, PendingSelection.TIMEOUT_MS));
        assertEquals(1, p.shown(1));
        assertEquals(PendingSelection.Result.NONE, p.reconcile(1, PendingSelection.TIMEOUT_MS + 10));
        assertNull(p.refusal(), "no server answer: the reason is unknown");
    }

    @Test
    void serverRefusalIsImmediateAndNamed() {
        PendingSelection p = new PendingSelection();
        int base = DestinationSelection.encodeOutcome(3, Outcome.OK);
        p.begin(7, 4, base, 0L);
        assertEquals(PendingSelection.Result.NONE, p.reconcile(4, base, 10L), "no answer yet");
        assertEquals(PendingSelection.Result.REFUSED,
                p.reconcile(4, DestinationSelection.encodeOutcome(4, Outcome.GATE_LOCKED), 20L));
        assertEquals(Outcome.GATE_LOCKED, p.refusal());
    }

    @Test
    void acceptedAnswerWaitsForTheIndex() {
        PendingSelection p = new PendingSelection();
        p.begin(7, 4, DestinationSelection.NO_OUTCOME, 0L);
        int ok = DestinationSelection.encodeOutcome(1, Outcome.OK);
        assertEquals(PendingSelection.Result.NONE, p.reconcile(4, ok, 10L));
        assertEquals(PendingSelection.Result.CONFIRMED, p.reconcile(7, ok, 20L));
    }

    @Test
    void stepThatWentNowhereIsRefusedAsOnlyOne() {
        PendingSelection p = new PendingSelection();
        p.begin(7, 4, DestinationSelection.NO_OUTCOME, 0L);
        assertEquals(PendingSelection.Result.REFUSED,
                p.reconcile(4, DestinationSelection.encodeOutcome(1, Outcome.ALREADY_SELECTED), 10L));
        assertEquals("gui.nerospace.dest.reason.only_one", DestinationSelection.refusalKey(p.refusal()));
    }
}
