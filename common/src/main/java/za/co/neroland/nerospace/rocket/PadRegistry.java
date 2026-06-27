package za.co.neroland.nerospace.rocket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Server-global registry of launch pads commissioned as named travel nodes. A rocket arriving in a
 * dimension lands on the nearest registered pad there (the player builds and registers a pad wherever
 * they want their landing spot — see {@link RocketLaunchPadBlock}), so launch pads double as the
 * waypoints of pad-to-pad travel. Stored on the overworld via the same {@link SavedDataType} codec
 * pattern as {@link StationRegistry}.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> entries store NO player identity — only an auto id, a label, the
 * dimension id, and the block position. Pads are server-global like stations.</p>
 */
public final class PadRegistry extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "pads");

    /** Hard cap so a grief-spammed registry can't bloat the save. */
    public static final int MAX_PADS = 256;

    public static final SavedDataType<PadRegistry> TYPE = new SavedDataType<>(
            ID, PadRegistry::new, codec(), null);

    /** One commissioned pad: an id (never reused), a label, its dimension id, and its block position. */
    public record PadNode(int id, String name, String dim, BlockPos pos) {

        public static final Codec<PadNode> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("id").forGetter(PadNode::id),
                Codec.STRING.fieldOf("name").forGetter(PadNode::name),
                Codec.STRING.fieldOf("dim").forGetter(PadNode::dim),
                BlockPos.CODEC.fieldOf("pos").forGetter(PadNode::pos)
        ).apply(inst, PadNode::of));

        private static PadNode of(Integer id, String name, String dim, BlockPos pos) {
            return new PadNode(id.intValue(), name, dim, pos);
        }
    }

    /** Insertion-ordered (commissioning order). */
    private final Map<Integer, PadNode> pads = new LinkedHashMap<>();
    private int nextId;

    public PadRegistry() {
    }

    private static Codec<PadRegistry> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                PadNode.CODEC.listOf().fieldOf("pads").forGetter(r -> new ArrayList<>(r.pads.values())),
                Codec.INT.fieldOf("next_id").forGetter(r -> r.nextId)
        ).apply(inst, PadRegistry::fromEntries));
    }

    private static PadRegistry fromEntries(List<PadNode> entries, Integer nextId) {
        PadRegistry registry = new PadRegistry();
        for (PadNode node : entries) {
            registry.pads.put(node.id(), node);
        }
        registry.nextId = nextId.intValue();
        return registry;
    }

    public static PadRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private static String dimId(ResourceKey<Level> dim) {
        return dim.identifier().toString();
    }

    /**
     * Registers (or renames, if one already exists at this exact spot) a pad node. A blank name
     * auto-labels "Pad N".
     *
     * @return the node, or {@code null} when {@link #MAX_PADS} is reached for a brand-new pad.
     */
    @Nullable
    public PadNode register(@Nullable String name, ResourceKey<Level> dim, BlockPos pos) {
        String dimId = dimId(dim);
        BlockPos at = pos.immutable();
        for (PadNode existing : this.pads.values()) {
            if (existing.dim().equals(dimId) && existing.pos().equals(at)) {
                String renamed = name == null || name.isBlank() ? existing.name() : name;
                PadNode updated = new PadNode(existing.id(), renamed, dimId, at);
                this.pads.put(existing.id(), updated);
                setDirty();
                return updated;
            }
        }
        if (this.pads.size() >= MAX_PADS) {
            return null;
        }
        int id = this.nextId++;
        String label = name == null || name.isBlank() ? "Pad " + (id + 1) : name;
        PadNode node = new PadNode(id, label, dimId, at);
        this.pads.put(id, node);
        setDirty();
        return node;
    }

    /** Unregisters the node at {@code (dim, pos)}; @return the removed node, or {@code null}. */
    @Nullable
    public PadNode unregisterAt(ResourceKey<Level> dim, BlockPos pos) {
        String dimId = dimId(dim);
        BlockPos at = pos.immutable();
        Integer key = null;
        for (PadNode node : this.pads.values()) {
            if (node.dim().equals(dimId) && node.pos().equals(at)) {
                key = node.id();
                break;
            }
        }
        if (key == null) {
            return null;
        }
        PadNode removed = this.pads.remove(key);
        setDirty();
        return removed;
    }

    public List<PadNode> all() {
        return List.copyOf(this.pads.values());
    }

    /** The registered pads in {@code dim}, in commissioning order. */
    public List<PadNode> inDimension(ResourceKey<Level> dim) {
        String dimId = dimId(dim);
        List<PadNode> out = new ArrayList<>();
        for (PadNode node : this.pads.values()) {
            if (node.dim().equals(dimId)) {
                out.add(node);
            }
        }
        return out;
    }

    /** The registered pad in {@code dim} closest to {@code to}, or {@code null} if none is registered. */
    @Nullable
    public PadNode nearest(ResourceKey<Level> dim, BlockPos to) {
        String dimId = dimId(dim);
        PadNode best = null;
        double bestSq = Double.MAX_VALUE;
        for (PadNode node : this.pads.values()) {
            if (!node.dim().equals(dimId)) {
                continue;
            }
            double sq = node.pos().distSqr(to);
            if (sq < bestSq) {
                bestSq = sq;
                best = node;
            }
        }
        return best;
    }
}
