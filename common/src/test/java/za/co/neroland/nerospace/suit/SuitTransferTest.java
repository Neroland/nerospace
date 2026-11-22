package za.co.neroland.nerospace.suit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SuitTransferTest {

    @Test
    void installAndRemoveDoNotCreateItems() {
        SuitLoadout loadout = SuitLoadout.EMPTY.withFrame(SuitFrame.MK2);
        var installed = SuitTransfers.install(loadout, SuitPiece.CHEST, SuitIds.POWER_CELL, 1);
        assertTrue(installed.applied());
        assertEquals(0, installed.cursorCount());
        assertEquals(1, installed.loadout().moduleCount(SuitIds.POWER_CELL));
        var removed = SuitTransfers.remove(installed.loadout(), SuitSlotKind.MODULE, 0);
        assertEquals(1, removed.cursorCount());
        assertEquals(0, removed.loadout().modules().size());
        var again = SuitTransfers.install(loadout, SuitPiece.CHEST, SuitIds.POWER_CELL, 0);
        assertEquals(0, again.cursorCount());
        assertEquals(loadout, again.loadout());
    }
}
