package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

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
    }
}
