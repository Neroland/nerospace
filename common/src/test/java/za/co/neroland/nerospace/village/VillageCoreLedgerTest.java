package za.co.neroland.nerospace.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;

/** The saved Village Core index + pending-erasure list: bounded, position-only pending, no raw UUID on disk. */
class VillageCoreLedgerTest {

    private static final long TODAY = 20_000L;

    private static VillageCoreLedger.CoreLoc at(long pos) {
        return new VillageCoreLedger.CoreLoc("nerospace:greenxertz", pos);
    }

    private static VillageCoreLedger roundTrip(VillageCoreLedger l) {
        JsonElement json = VillageCoreLedger.storeCodec().encodeStart(JsonOps.INSTANCE, l).getOrThrow();
        return VillageCoreLedger.storeCodec().parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    @Test
    void encodedLedgerHoldsNoRawUuid() {
        UUID owner = UUID.randomUUID();
        VillageCoreLedger l = new VillageCoreLedger();
        l.record(at(1), owner);
        l.forgetOwner(UUID.randomUUID(), x -> false, TODAY);
        String json = VillageCoreLedger.storeCodec().encodeStart(JsonOps.INSTANCE, l).getOrThrow().toString();
        assertFalse(json.contains(owner.toString()));
        assertFalse(json.contains(owner.toString().replace("-", "")));
    }

    @Test
    void roundTripKeepsSaltRowsPendingAndLegacy() {
        UUID owner = UUID.randomUUID();
        VillageCoreLedger l = new VillageCoreLedger();
        l.record(at(1), owner);
        l.record(at(2), owner);
        l.forgetOwner(owner, x -> x.equals(at(1)), TODAY);
        l.record(at(3), owner); // re-claimed after erasure
        VillageCoreLedger back = roundTrip(l);
        assertEquals(l.keyOf(owner), back.keyOf(owner), "salt persisted");
        assertTrue(back.isPending(at(2)));
        assertFalse(back.isPending(at(1)));
        assertEquals(List.of(at(3)), back.ownedBy(owner));
        assertEquals(1, back.legacyCount());
    }

    @Test
    void legacyCoreNeverIndexedIsClearedOnFirstLoadUntilTtl() {
        UUID owner = UUID.randomUUID();
        VillageCoreLedger l = new VillageCoreLedger();
        l.forgetOwner(owner, x -> false, TODAY);
        assertTrue(l.onLoaded(at(9), owner, TODAY + 1), "unindexed 1.x core with an erased owner");
        assertFalse(l.retainsData(owner));
        VillageCoreLedger expired = new VillageCoreLedger();
        expired.forgetOwner(owner, x -> false, TODAY);
        assertFalse(expired.onLoaded(at(9), owner, TODAY + VillageCoreLedger.LEGACY_TTL_DAYS + 1),
                "past the TTL the legacy key is gone");
        assertEquals(0, expired.legacyCount(), "expired legacy key pruned");
    }

    @Test
    void indexedCoreOfAnotherOwnerIsNeverCleared() {
        UUID erased = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        VillageCoreLedger l = new VillageCoreLedger();
        l.record(at(1), other);
        l.forgetOwner(erased, x -> false, TODAY);
        assertFalse(l.onLoaded(at(1), other, TODAY));
        assertEquals(List.of(at(1)), l.ownedBy(other));
    }

    @Test
    void listsAreBounded() {
        VillageCoreLedger l = new VillageCoreLedger();
        for (int i = 0; i < VillageCoreLedger.MAX_LEGACY + 10; i++) {
            l.forgetOwner(UUID.randomUUID(), x -> false, TODAY);
        }
        assertEquals(VillageCoreLedger.MAX_LEGACY, l.legacyCount());
        UUID owner = UUID.randomUUID();
        for (int i = 0; i < VillageCoreLedger.MAX_PENDING + 5; i++) {
            l.record(at(i), owner);
        }
        l.forgetOwner(owner, x -> false, TODAY);
        assertEquals(VillageCoreLedger.MAX_PENDING, l.pendingCount());
    }

    @Test
    void brokenCoreDropsRowAndPending() {
        UUID owner = UUID.randomUUID();
        VillageCoreLedger l = new VillageCoreLedger();
        l.record(at(1), owner);
        l.forgetOwner(owner, x -> false, TODAY);
        l.remove(at(1));
        assertEquals(0, l.pendingCount());
    }
}
