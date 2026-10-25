package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.mojang.serialization.Dynamic;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.api.NerospacePlanets;
import za.co.neroland.nerospace.api.StationInfo;
import za.co.neroland.nerospace.world.station.DockingPolicy;
import za.co.neroland.nerospace.world.station.ModuleKind;
import za.co.neroland.nerospace.world.station.StationAccess;
import za.co.neroland.nerospace.world.station.StationClass;

/**
 * A 1.6.0 {@code nerospace:stations} store (an owned and a blank-owner station, {@code next_slot 2}) loads with every
 * 2.0 field at its default and round-trips to a fixed point; lenient enums and dropped unknown modules.
 */
class StationRegistryMigrationTest {

    static final String OWNER = "4b2b9a4e-1c2d-4e5f-8a9b-0c1d2e3f4a5b";

    /** The 1.6.0 shape: {stations:[{slot,name,center:[I;x,y,z],owner?}], next_slot}. */
    static CompoundTag legacyStore() {
        ListTag stations = new ListTag();
        stations.add(entry(0, "Home Station", 4096, OWNER));
        stations.add(entry(1, "Station 2", 8192, null));
        CompoundTag root = new CompoundTag();
        root.put("stations", stations);
        root.putInt("next_slot", 2);
        return root;
    }

    static CompoundTag entry(int slot, String name, int x, String owner) {
        CompoundTag t = new CompoundTag();
        t.putInt("slot", slot);
        t.putString("name", name);
        t.put("center", new IntArrayTag(new int[] { x, 64, 0 }));
        if (owner != null) {
            t.putString("owner", owner);
        }
        return t;
    }

    static StationRegistry decode(Tag tag) {
        return StationRegistry.codec().parse(new Dynamic<>(NbtOps.INSTANCE, tag)).getOrThrow();
    }

    @Test
    void legacyStationsGetDefaults() {
        StationRegistry r = decode(legacyStore());
        assertEquals(2, r.count());
        for (StationRegistry.StationEntry e : r.all()) {
            assertEquals(StationClass.WAYSTATION, e.stationClass());
            assertTrue(e.members().isEmpty());
            assertTrue(e.modules().isEmpty());
            assertEquals(-1L, e.foundedAt());
            assertEquals(-1L, e.classChangedAt());
            assertEquals(DockingPolicy.OPEN, e.docking());
        }
        assertEquals(OWNER, r.get(0).owner());
        assertEquals("", r.get(1).owner());
        assertEquals(new BlockPos(4096, 64, 0), r.get(0).center());
        assertEquals(StationRegistry.centerFor(1), r.get(1).center());
    }

    @Test
    void canManageMatchesThe16Rules() {
        StationRegistry r = decode(legacyStore());
        String stranger = "00000000-0000-0000-0000-000000000001";
        assertTrue(StationAccess.canManage(r.get(1).owner(), stranger, false), "blank owner: anyone");
        assertTrue(StationAccess.canManage(r.get(0).owner(), OWNER, false));
        assertFalse(StationAccess.canManage(r.get(0).owner(), stranger, false));
    }

    @Test
    void roundTripIsAFixedPoint() {
        StationRegistry r = decode(legacyStore());
        Tag once = StationRegistry.codec().encodeStart(NbtOps.INSTANCE, r).getOrThrow();
        Tag twice = StationRegistry.codec().encodeStart(NbtOps.INSTANCE, decode(once)).getOrThrow();
        assertEquals(once, twice);
        // 2.0 fields survive a round trip too.
        StationRegistry.StationEntry e = r.get(0).withClass(StationClass.TRADE, 1234L)
                .withMembers(List.of("00000000-0000-0000-0000-000000000002")).withDocking(DockingPolicy.MEMBERS)
                .withFoundedAt(20_000L);
        r.put(e);
        StationRegistry back = decode(StationRegistry.codec().encodeStart(NbtOps.INSTANCE, r).getOrThrow());
        assertEquals(e, back.get(0));
    }

    @Test
    void bogusEnumsAreLenientAndUnknownModulesDropped() {
        CompoundTag root = legacyStore();
        CompoundTag bad = (CompoundTag) ((ListTag) root.get("stations")).get(0);
        bad.putString("class", "BOGUS");
        bad.putString("docking", "BOGUS");
        ListTag modules = new ListTag();
        CompoundTag good = new CompoundTag();
        good.putString("kind", "HABITAT_RING");
        good.put("anchor", new IntArrayTag(new int[] { 4086, 64, 0 }));
        CompoundTag unknown = new CompoundTag();
        unknown.putString("kind", "WARP_DRIVE");
        unknown.put("anchor", new IntArrayTag(new int[] { 0, 0, 0 }));
        modules.add(good);
        modules.add(unknown);
        bad.put("modules", modules);
        StationRegistry r = decode(root);
        StationRegistry.StationEntry e = r.get(0);
        assertEquals(StationClass.WAYSTATION, e.stationClass());
        assertEquals(DockingPolicy.OPEN, e.docking());
        assertEquals(1, e.modules().size());
        assertEquals(ModuleKind.HABITAT_RING, e.modules().get(0).kind());
        assertTrue(e.modules().get(0).enabled(), "enabled defaults to true");
    }

    @Test
    void migratedEntryReportsWaystationInfo() {
        StationRegistry.StationEntry e = decode(legacyStore()).get(0);
        StationInfo info = new StationInfo(e.slot(), e.name(), NerospacePlanets.STATION, e.center(),
                e.stationClass().traits().routeCapacity(), e.stationClass().name());
        assertEquals(9, info.routeCapacity());
        assertEquals("WAYSTATION", info.stationClass());
    }
}
