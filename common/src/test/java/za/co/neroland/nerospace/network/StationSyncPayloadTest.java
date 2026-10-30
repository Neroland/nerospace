package za.co.neroland.nerospace.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.List;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.rocket.StationDockingRule;
import za.co.neroland.nerospace.rocket.StationRegistry.StationEntry;
import za.co.neroland.nerospace.world.station.DockingPolicy;
import za.co.neroland.nerospace.world.station.StationAccess;

/** 005 §4: a MEMBERS-only station's name is not sent to a viewer who cannot use it. */
class StationSyncPayloadTest {

    private static final String OWNER = "00000000-0000-0000-0000-000000000001";
    private static final String STRANGER = "00000000-0000-0000-0000-000000000009";

    @Test
    void membersOnlyStationsAreHiddenFromNonMembers() {
        StationEntry open = new StationEntry(0, "Open Port", new BlockPos(0, 64, 0), OWNER);
        StationEntry closed = new StationEntry(1, "Private Dock", new BlockPos(4096, 64, 0), OWNER)
                .withDocking(DockingPolicy.MEMBERS);
        List<StationEntry> all = List.of(open, closed);

        StationSyncPayload stranger = StationSyncPayload.of(all, e -> StationDockingRule.mayChoose(e.docking(),
                StationAccess.canUse(e.owner(), e.members(), STRANGER, false)));
        assertArrayEquals(new int[] { 0 }, stranger.slots());
        assertArrayEquals(new String[] { "Open Port" }, stranger.names());

        StationSyncPayload owner = StationSyncPayload.of(all, e -> StationDockingRule.mayChoose(e.docking(),
                StationAccess.canUse(e.owner(), e.members(), OWNER, false)));
        assertArrayEquals(new int[] { 0, 1 }, owner.slots());
    }
}
