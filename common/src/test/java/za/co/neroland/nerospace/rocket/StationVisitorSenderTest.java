package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Send-home moves visitor rockets only — never the owner's or a member's own, and never while a visitor is aboard. */
class StationVisitorSenderTest {

    private static final String OWNER = "00000000-0000-0000-0000-000000000001";
    private static final String MEMBER = "00000000-0000-0000-0000-000000000002";
    private static final String STRANGER = "00000000-0000-0000-0000-000000000003";

    @Test
    void ownerOrMemberRocketIsNeverSent() {
        // The owner (or a member) landed: canUse at arrival → not a visitor → never eligible.
        boolean visitor = StationVisitorSender.isVisitor(true, true, true, true);
        assertFalse(visitor);
        assertFalse(StationVisitorSender.eligible(visitor, false, 1_000_000L, true, false));
    }

    @Test
    void strangerRocketIsAVisitor() {
        boolean visitor = StationVisitorSender.isVisitor(true, true, true, false);
        assertTrue(visitor);
        assertTrue(StationVisitorSender.eligible(visitor, false, StationVisitorSender.MIN_PARKED_TICKS, true, false));
        assertFalse(StationVisitorSender.eligible(visitor, true, 1_000_000L, true, false)); // crewed: never
    }

    @Test
    void nothingIsSentWhileANonMemberIsAboard() {
        // The visitor walked off their rocket and is exploring the station: their rocket is not abandoned.
        assertFalse(StationVisitorSender.eligible(true, false, 1_000_000L, true, true));
    }

    @Test
    void onlyPlayersWhoCannotUseTheStationHoldTheSend() {
        List<String> members = List.of(MEMBER);
        assertFalse(StationVisitorSender.holdsSend(OWNER, members, OWNER, false, false));
        assertFalse(StationVisitorSender.holdsSend(OWNER, members, MEMBER, false, false));
        assertFalse(StationVisitorSender.holdsSend(OWNER, members, STRANGER, true, false)); // operator
        assertTrue(StationVisitorSender.holdsSend(OWNER, members, STRANGER, false, false));
        assertFalse(StationVisitorSender.holdsSend(OWNER, members, STRANGER, false, true)); // spectator: not stranded
        assertFalse(StationVisitorSender.holdsSend("", members, STRANGER, false, false));   // unowned: everyone can use
    }

    @Test
    void nonStationAndUncrewedArrivalsAreNotVisitors() {
        assertFalse(StationVisitorSender.isVisitor(false, false, true, false)); // planet arrival
        assertFalse(StationVisitorSender.isVisitor(true, false, true, false));  // origin platform, no entry
        assertFalse(StationVisitorSender.isVisitor(true, true, false, false));  // uncrewed send-home arrival
    }

    @Test
    void unflaggedRocketsAreNeverSent() {
        // A 1.6.0 rocket (or one that landed before the flag existed) loads with Visitor=false.
        assertFalse(StationVisitorSender.eligible(false, false, 1_000_000L, true, false));
    }
}
