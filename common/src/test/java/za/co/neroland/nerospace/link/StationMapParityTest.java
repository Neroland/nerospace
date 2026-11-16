package za.co.neroland.nerospace.link;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.map.NerospaceMapLayers;
import za.co.neroland.nerospace.rocket.StationRegistry;

/**
 * Rule G6: the map layer re-implements the link module's station rule, so both must answer the same for an
 * owner, a member, a stranger and an erased owner.
 */
class StationMapParityTest {

    @Test
    void mapAndLinkAgreeOnStationVisibility() {
        String owner = UUID.randomUUID().toString();
        String member = UUID.randomUUID().toString();
        String stranger = UUID.randomUUID().toString();
        StationRegistry r = new StationRegistry();
        int slot = r.found("Alpha", owner).slot();
        r.addMember(slot, member);
        StationRegistry.StationEntry live = r.get(slot);
        for (String who : new String[] { owner, member, stranger, "", null }) {
            assertEquals(NerospaceLinkAccess.visibleTo(live, who), NerospaceMapLayers.stationVisibleTo(live, who));
        }
        r.forgetPlayer(UUID.fromString(owner));
        StationRegistry.StationEntry erased = r.get(slot);
        for (String who : new String[] { owner, member, stranger }) {
            assertEquals(NerospaceLinkAccess.visibleTo(erased, who),
                    NerospaceMapLayers.stationVisibleTo(erased, who));
        }
    }
}
