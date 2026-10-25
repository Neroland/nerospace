package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.data.ErasureConformance;
import za.co.neroland.nerolandcore.data.PlayerDataEraser;
import za.co.neroland.nerolandcore.data.PlayerDataErasure;
import za.co.neroland.nerospace.world.station.StationAccess;

/** The owner / member / operator / stranger × owned / unowned matrix, members, the class cooldown and erasure. */
class StationAccessTest {

    static final String OWNER = "11111111-1111-1111-1111-111111111111";
    static final String MEMBER = "22222222-2222-2222-2222-222222222222";
    static final String STRANGER = "33333333-3333-3333-3333-333333333333";
    static final List<String> MEMBERS = List.of(MEMBER);

    @Test
    void manageAndUseMatrix() {
        // {actor, op, owned→manage, owned→use, unowned→manage, unowned→use}
        Object[][] table = {
                { OWNER, false, true, true, true, true },
                { MEMBER, false, false, true, true, true },
                { STRANGER, true, true, true, true, true },
                { STRANGER, false, false, false, true, true },
        };
        for (Object[] row : table) {
            String actor = (String) row[0];
            boolean op = (Boolean) row[1];
            assertEquals(row[2], StationAccess.canManage(OWNER, actor, op), "manage owned " + actor + " op=" + op);
            assertEquals(row[3], StationAccess.canUse(OWNER, MEMBERS, actor, op), "use owned " + actor);
            assertEquals(row[4], StationAccess.canManage("", actor, op), "manage unowned " + actor);
            assertEquals(row[5], StationAccess.canUse("", List.of(), actor, op), "use unowned " + actor);
        }
    }

    @Test
    void operatorBranchIsDetectedForAuditOnly() {
        assertTrue(StationAccess.manageViaOp(OWNER, STRANGER, true));
        assertFalse(StationAccess.manageViaOp(OWNER, OWNER, true), "an op managing their own station is not logged");
        assertFalse(StationAccess.manageViaOp("", STRANGER, true), "unowned stations are anyone's");
        assertFalse(StationAccess.useViaOp(OWNER, MEMBERS, MEMBER, true));
        assertTrue(StationAccess.useViaOp(OWNER, MEMBERS, STRANGER, true));
    }

    @Test
    void membersAreCappedAndClean() {
        StationRegistry r = new StationRegistry();
        StationRegistry.StationEntry e = r.found("Alpha", OWNER);
        int slot = e.slot();
        assertFalse(r.addMember(slot, OWNER), "the owner is never a member");
        for (int i = 0; i < StationAccess.MAX_MEMBERS; i++) {
            assertTrue(r.addMember(slot, new UUID(7L, i).toString()));
        }
        assertFalse(r.addMember(slot, new UUID(7L, 99).toString()), "the 17th is refused");
        assertFalse(r.addMember(slot, new UUID(7L, 0).toString()), "a duplicate is refused");
        assertEquals(StationAccess.MAX_MEMBERS, r.get(slot).members().size());
        assertTrue(r.removeMember(slot, new UUID(7L, 0).toString()));
        assertFalse(r.removeMember(slot, new UUID(7L, 0).toString()), "remove is idempotent");
        assertEquals(StationAccess.AddVerdict.NOT_ONLINE,
                StationAccess.canAddMember(OWNER, List.of(), MEMBER, false));
    }

    @Test
    void classCooldown() {
        assertTrue(StationAccess.canChangeClass(5L, -1L, false), "first change is free");
        assertFalse(StationAccess.canChangeClass(23_999L, 0L, false));
        assertTrue(StationAccess.canChangeClass(24_000L, 0L, false));
        assertTrue(StationAccess.canChangeClass(1L, 0L, true), "operators bypass");
        assertEquals(1L, StationAccess.classCooldownRemaining(23_999L, 0L));
    }

    @Test
    void foundingStampsTheDayNotATimestamp() {
        StationRegistry r = new StationRegistry();
        StationRegistry.StationEntry e = r.found(null, OWNER);
        assertTrue(e.foundedAt() > 19_000L && e.foundedAt() < 100_000L, "an epoch day: " + e.foundedAt());
        assertEquals(StationRegistry.centerFor(0), e.center());
    }

    @Test
    void erasureStripsMembersEverywhereAndBlanksOwners() {
        StationRegistry r = new StationRegistry();
        UUID erased = UUID.fromString(MEMBER);
        int a = r.found("Alpha", OWNER).slot();
        int b = r.found("Beta", STRANGER).slot();
        int c = r.found("Gamma", MEMBER).slot();
        r.addMember(a, MEMBER);
        r.addMember(b, MEMBER);
        r.addMember(b, OWNER);
        assertTrue(r.retainsData(erased));

        PlayerDataEraser eraser = (server, uuid) -> r.forgetPlayer(uuid);
        PlayerDataErasure.register(eraser);
        try {
            ErasureConformance.create().probe("nerospace:stations", r::retainsData).verify(null, erased);
        } finally {
            PlayerDataErasure.unregister(eraser);
        }
        assertFalse(r.retainsData(erased));
        assertTrue(r.get(a).members().isEmpty());
        assertEquals(List.of(OWNER), r.get(b).members(), "other members are kept");
        assertEquals("", r.get(c).owner(), "owner anonymised, station kept");
        assertEquals("Gamma", r.get(c).name());
        assertEquals(new BlockPos(StationRegistry.centerFor(c).getX(), 64, 0), r.get(c).center());
        assertEquals(0, r.forgetPlayer(erased), "a second pass touches nothing");
    }
}
