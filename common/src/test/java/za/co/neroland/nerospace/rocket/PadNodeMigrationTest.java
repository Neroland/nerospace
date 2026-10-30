package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.mojang.serialization.Dynamic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import org.junit.jupiter.api.Test;

/** A 1.6.0 {@code nerospace:pads} store loads with the 2.0 defaults and round-trips. */
class PadNodeMigrationTest {

    /** The 1.6.0 shape: {pads:[{id,name,dim,pos:[I;x,y,z]}], next_id}. */
    static CompoundTag legacyStore() {
        ListTag pads = new ListTag();
        pads.add(node(0, "Home", "minecraft:overworld", 10, 70, -4));
        pads.add(node(1, "Ember Base", "nerospace:cindara", -200, 88, 31));
        CompoundTag root = new CompoundTag();
        root.put("pads", pads);
        root.putInt("next_id", 2);
        return root;
    }

    static CompoundTag node(int id, String name, String dim, int x, int y, int z) {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("name", name);
        t.putString("dim", dim);
        t.put("pos", new IntArrayTag(new int[] { x, y, z }));
        return t;
    }

    static PadRegistry decode(Tag tag) {
        return PadRegistry.codec().parse(new Dynamic<>(NbtOps.INSTANCE, tag)).getOrThrow();
    }

    @Test
    void legacyNodesGetDefaults() {
        PadRegistry r = decode(legacyStore());
        List<PadRegistry.PadNode> all = r.all();
        assertEquals(2, all.size());
        for (PadRegistry.PadNode n : all) {
            assertEquals(PadKind.LAUNCH_PAD, n.kind());
            assertTrue(n.center().isEmpty());
            assertTrue(n.facing().isEmpty());
        }
        assertEquals(new BlockPos(10, 70, -4), all.get(0).pos());
        assertEquals("Ember Base", all.get(1).name());
    }

    @Test
    void roundTripIsStable() {
        PadRegistry r = decode(legacyStore());
        Tag once = PadRegistry.codec().encodeStart(NbtOps.INSTANCE, r).getOrThrow();
        Tag twice = PadRegistry.codec().encodeStart(NbtOps.INSTANCE, decode(once)).getOrThrow();
        assertEquals(once, twice);
    }

    @Test
    void fillWritesOnceAndNeverMovesPos() {
        PadRegistry r = decode(legacyStore());
        assertTrue(r.fill(0, new BlockPos(11, 70, -3), Direction.EAST));
        PadRegistry.PadNode n = r.byId(0);
        assertEquals(new BlockPos(10, 70, -4), n.pos());
        assertEquals(new BlockPos(11, 70, -3), n.center().orElseThrow());
        // A second fill never overwrites what is there.
        assertTrue(!r.fill(0, new BlockPos(99, 70, 99), Direction.NORTH));
        assertEquals(Direction.EAST, r.byId(0).facing().orElseThrow());
        // And the filled fields survive a round trip.
        Tag tag = PadRegistry.codec().encodeStart(NbtOps.INSTANCE, r).getOrThrow();
        assertEquals(Direction.EAST, decode(tag).byId(0).facing().orElseThrow());
    }

    @Test
    void unknownKindLoadsAsLaunchPad() {
        CompoundTag root = legacyStore();
        ((CompoundTag) root.getListOrEmpty("pads").get(0)).putString("kind", "WARP_GATE");
        assertEquals(PadKind.LAUNCH_PAD, decode(root).byId(0).kind());
    }

    @Test
    void podNodesHaveTheirOwnCapAndStayOutOfTheCycler() {
        PadRegistry r = decode(legacyStore());
        var dim = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.Identifier.parse("nerospace:cindara"));
        for (int i = 0; i < PadRegistry.MAX_POD_NODES + 5; i++) {
            r.register(null, dim, new BlockPos(i * 40, 64, 0), PadKind.POD, null, null);
        }
        assertEquals(PadRegistry.MAX_POD_NODES, r.podNodesIn(dim).size());
        assertEquals(1, r.inDimension(dim).size()); // only "Ember Base"
    }
}
