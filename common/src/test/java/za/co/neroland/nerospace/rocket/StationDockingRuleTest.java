package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.station.DockingPolicy;
import za.co.neroland.nerospace.world.station.StationAccess;

/** 005 §4 docking policy (cycler + launch re-check) and the 005 G7 visitor send-home eligibility. */
class StationDockingRuleTest {

    private static final String OWNER = "00000000-0000-0000-0000-00000000000a";
    private static final String MEMBER = "00000000-0000-0000-0000-00000000000b";
    private static final String STRANGER = "00000000-0000-0000-0000-00000000000c";

    static boolean may(DockingPolicy policy, String owner, String actor, boolean op) {
        return StationDockingRule.mayChoose(policy,
                StationAccess.canUse(owner, java.util.List.of(MEMBER), actor, op));
    }

    @Test
    void openStationsAreOpenToAll() {
        assertTrue(may(DockingPolicy.OPEN, OWNER, STRANGER, false));
    }

    @Test
    void membersOnlyStations() {
        assertTrue(may(DockingPolicy.MEMBERS, OWNER, OWNER, false));
        assertTrue(may(DockingPolicy.MEMBERS, OWNER, MEMBER, false));
        assertTrue(may(DockingPolicy.MEMBERS, OWNER, STRANGER, true)); // op
        assertFalse(may(DockingPolicy.MEMBERS, OWNER, STRANGER, false));
        assertTrue(may(DockingPolicy.MEMBERS, "", STRANGER, false)); // unowned legacy stations stay everyone's
    }

    @Test
    void visitorSendHomeEligibility() {
        assertTrue(StationVisitorSender.eligible(true, false, 12_000L, true));
        assertFalse(StationVisitorSender.eligible(true, false, 11_999L, true));
        assertFalse(StationVisitorSender.eligible(true, true, 50_000L, true)); // never move a crewed rocket
        assertFalse(StationVisitorSender.eligible(true, false, 50_000L, false)); // not blocking a dock
        assertFalse(StationVisitorSender.eligible(true, false, -1L, true)); // unknown parking time
    }
}
