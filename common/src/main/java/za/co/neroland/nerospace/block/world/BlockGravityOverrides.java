package za.co.neroland.nerospace.block.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.block.GravityOverrideBlock;
import za.co.neroland.nerospace.block.rules.BlockGravityRules;
import za.co.neroland.nerospace.block.rules.BlockGravityRules.Kind;
import za.co.neroland.nerospace.config.NerospaceConfig;

/**
 * Per-level block gravity overrides ({@code nerospace:block_gravity}, decision 002 §10.4): block position → kind,
 * maintained on place/break by {@code GravityOverrideBlock}. {@code GravityManager.factorAt} consults it before the
 * biome tags. Lookups are chunk-local (≤ 64 entries per chunk, plus the neighbouring chunk when the queried cell is
 * on a chunk edge). Holds block positions only — no player data.
 */
public final class BlockGravityOverrides extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "block_gravity");
    public static final String RECOVERY_NAME = "nerospace:block_gravity";

    /** Sentinel for a malformed entry with no position; skipped on load instead of failing the whole list. */
    private static final long NO_POS = Long.MIN_VALUE;

    private record Entry(long pos, String kind) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.optionalFieldOf("pos", NO_POS).forGetter(Entry::pos),
                Codec.STRING.optionalFieldOf("kind", Kind.SHARD.name()).forGetter(Entry::kind)
        ).apply(i, Entry::new));
    }

    private static final Codec<BlockGravityOverrides> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("overrides", List.of()).forGetter(BlockGravityOverrides::entries)
    ).apply(i, BlockGravityOverrides::of));

    public static final SavedDataType<BlockGravityOverrides> TYPE =
            new SavedDataType<>(ID, BlockGravityOverrides::new, CODEC, null);

    private static final Map<ServerLevel, BlockGravityOverrides> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    /** chunk key → (block pos → kind). */
    private final Long2ObjectOpenHashMap<Long2ObjectOpenHashMap<Kind>> byChunk = new Long2ObjectOpenHashMap<>();
    private int size;

    public BlockGravityOverrides() {
    }

    private static BlockGravityOverrides of(List<Entry> entries) {
        BlockGravityOverrides s = new BlockGravityOverrides();
        for (Entry e : entries) {
            Kind kind = Kind.byName(e.kind());
            if (kind != null && e.pos() != NO_POS) {
                s.put(e.pos(), kind);
            }
        }
        return s;
    }

    private List<Entry> entries() {
        List<Entry> out = new ArrayList<>(this.size);
        for (Long2ObjectOpenHashMap<Kind> chunk : this.byChunk.values()) {
            chunk.long2ObjectEntrySet().forEach(e -> out.add(new Entry(e.getLongKey(), e.getValue().name())));
        }
        return out;
    }

    public static BlockGravityOverrides get(ServerLevel level) {
        return CACHE.computeIfAbsent(level,
                l -> SavedDataRecovery.get(l, TYPE, BlockGravityOverrides::new, RECOVERY_NAME));
    }

    private static long chunkKey(int blockX, int blockZ) {
        return ChunkPos.pack(blockX >> 4, blockZ >> 4);
    }

    private void put(long pos, Kind kind) {
        long ck = chunkKey(BlockPos.getX(pos), BlockPos.getZ(pos));
        Kind prev = this.byChunk.computeIfAbsent(ck, k -> new Long2ObjectOpenHashMap<>()).put(pos, kind);
        if (prev == null) {
            this.size++;
        }
    }

    /** Overrides registered in the chunk holding {@code pos}. */
    public int countInChunk(BlockPos pos) {
        Long2ObjectOpenHashMap<Kind> chunk = this.byChunk.get(chunkKey(pos.getX(), pos.getZ()));
        return chunk == null ? 0 : chunk.size();
    }

    /** Registers an override at a block; false (nothing stored) when the chunk is at its cap. */
    public boolean register(BlockPos pos, Kind kind) {
        long key = pos.asLong();
        Long2ObjectOpenHashMap<Kind> chunk = this.byChunk.get(chunkKey(pos.getX(), pos.getZ()));
        if (chunk != null && chunk.containsKey(key)) {
            chunk.put(key, kind);
            setDirty();
            return true;
        }
        if (!BlockGravityRules.canRegister(countInChunk(pos), NerospaceConfig.gravityOverrideCap())) {
            return false;
        }
        put(key, kind);
        setDirty();
        return true;
    }

    public void unregister(BlockPos pos) {
        long ck = chunkKey(pos.getX(), pos.getZ());
        Long2ObjectOpenHashMap<Kind> chunk = this.byChunk.get(ck);
        if (chunk != null && chunk.remove(pos.asLong()) != null) {
            this.size--;
            if (chunk.isEmpty()) {
                this.byChunk.remove(ck);
            }
            setDirty();
        }
    }

    /** Whether an override is registered at exactly this block. */
    public boolean contains(BlockPos pos) {
        Long2ObjectOpenHashMap<Kind> chunk = this.byChunk.get(chunkKey(pos.getX(), pos.getZ()));
        return chunk != null && chunk.containsKey(pos.asLong());
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    /** Every override whose column covers {@code cell} (searches the cell's chunk and the edge neighbours). */
    public List<BlockGravityRules.GravityOverride> coveringOverrides(BlockPos cell) {
        return coveringOverrides(cell, null);
    }

    /**
     * As {@link #coveringOverrides(BlockPos)}; with a level, a hit whose source block (in a loaded chunk) is no longer
     * a gravity block — removed by a world edit or structure overwrite that skipped the removal hook — is pruned.
     */
    public List<BlockGravityRules.GravityOverride> coveringOverrides(BlockPos cell, ServerLevel level) {
        if (this.size == 0) {
            return List.of();
        }
        List<BlockGravityRules.GravityOverride> out = null;
        List<Long> stale = null;
        int cx = cell.getX() >> 4;
        int cz = cell.getZ() >> 4;
        int lx = cell.getX() & 15;
        int lz = cell.getZ() & 15;
        for (int ox = (lx == 0 ? -1 : 0); ox <= (lx == 15 ? 1 : 0); ox++) {
            for (int oz = (lz == 0 ? -1 : 0); oz <= (lz == 15 ? 1 : 0); oz++) {
                Long2ObjectOpenHashMap<Kind> chunk = this.byChunk.get(ChunkPos.pack(cx + ox, cz + oz));
                if (chunk == null) {
                    continue;
                }
                for (var e : chunk.long2ObjectEntrySet()) {
                    long p = e.getLongKey();
                    Kind kind = e.getValue();
                    if (BlockGravityRules.inColumn(cell.getX() - BlockPos.getX(p), cell.getY() - BlockPos.getY(p),
                            cell.getZ() - BlockPos.getZ(p), kind)) {
                        if (level != null && isPhantom(level, p)) {
                            if (stale == null) {
                                stale = new ArrayList<>(1);
                            }
                            stale.add(p);
                            continue;
                        }
                        if (out == null) {
                            out = new ArrayList<>(2);
                        }
                        out.add(new BlockGravityRules.GravityOverride(kind, kind.factor()));
                    }
                }
            }
        }
        if (stale != null) {
            for (long p : stale) {
                unregister(BlockPos.of(p));
            }
        }
        return out == null ? List.of() : out;
    }

    private static boolean isPhantom(ServerLevel level, long packed) {
        BlockPos pos = BlockPos.of(packed);
        return level.isLoaded(pos) && !(level.getBlockState(pos).getBlock() instanceof GravityOverrideBlock);
    }

    /** The override factor at a cell, or {@code NaN} when no override covers it. */
    public static double factorAt(ServerLevel level, BlockPos cell) {
        BlockGravityOverrides store = get(level);
        if (store.isEmpty()) {
            return Double.NaN;
        }
        List<BlockGravityRules.GravityOverride> covering = store.coveringOverrides(cell, level);
        return covering.isEmpty() ? Double.NaN : BlockGravityRules.factor(Double.NaN, covering);
    }
}
