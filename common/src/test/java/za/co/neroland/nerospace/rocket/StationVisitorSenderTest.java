package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 005 G7 as ruled: send-home moves visitor rockets only — never the owner's or a member's own. */
class StationVisitorSenderTest {

    @Test
    void ownerOrMemberRocketIsNeverSent() {
        // The owner (or a member) landed: canUse at arrival → not a visitor → never eligible.
        boolean visitor = StationVisitorSender.isVisitor(true, true, true, true);
        assertFalse(visitor);
        assertFalse(StationVisitorSender.eligible(visitor, false, 1_000_000L, true));
    }

    @Test
    void strangerRocketIsAVisitor() {
        boolean visitor = StationVisitorSender.isVisitor(true, true, true, false);
        assertTrue(visitor);
        assertTrue(StationVisitorSender.eligible(visitor, false, StationVisitorSender.MIN_PARKED_TICKS, true));
        assertFalse(StationVisitorSender.eligible(visitor, true, 1_000_000L, true)); // crewed: never
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
        assertFalse(StationVisitorSender.eligible(false, false, 1_000_000L, true));
    }
}
