package za.co.neroland.nerospace.link;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.rocket.StationRegistry;

/** NeroLink shows a station — and therefore its member list — only to its owner and its members. */
class StationVisibilityTest {

    @Test
    void onlyOwnerAndMembersSeeAStation() {
        String owner = UUID.randomUUID().toString();
        String member = UUID.randomUUID().toString();
        String stranger = UUID.randomUUID().toString();
        StationRegistry r = new StationRegistry();
        int slot = r.found("Alpha", owner).slot();
        r.addMember(slot, member);
        StationRegistry.StationEntry e = r.get(slot);
        assertTrue(NerospaceLinkAccess.visibleTo(e, owner));
        assertTrue(NerospaceLinkAccess.visibleTo(e, member));
        assertFalse(NerospaceLinkAccess.visibleTo(e, stranger));
        assertFalse(NerospaceLinkAccess.visibleTo(e, null));
        assertFalse(NerospaceLinkAccess.visibleTo(e, ""));

        r.forgetPlayer(UUID.fromString(owner));
        StationRegistry.StationEntry anonymised = r.get(slot);
        assertFalse(NerospaceLinkAccess.visibleTo(anonymised, owner));
        assertFalse(NerospaceLinkAccess.visibleTo(anonymised, member), "an unowned station is visible to nobody");
    }
}
