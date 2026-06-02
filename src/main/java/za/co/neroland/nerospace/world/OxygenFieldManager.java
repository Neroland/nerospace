package za.co.neroland.nerospace.world;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;

/**
 * Per-{@link ServerLevel} oxygen field (terraform design §1). Holds a sparse per-block concentration
 * field (world-packed pos → {@code 0..MAX}) and the set of active oxygen sources, and runs a throttled
 * diffusion-with-decay relaxation that produces dissipation in open space, fill in sealed volumes, and
 * leakage through openings from one rule. Breathability is then an O(1) hash lookup.
 *
 * <p>The live field is held in memory (always loaded with the level) and re-converges from its sources
 * within a few seconds of load, so only the source set is persisted (via the {@link SavedDataType}
 * codec). The permanent terraformed-ground breathability is a separate per-chunk flag (see
 * {@code ModAttachments.TERRAFORMED}), never simulated.</p>
 */
public final class OxygenFieldManager extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Nerospace.MODID, "oxygen_field");

    public static final SavedDataType<OxygenFieldManager> TYPE = new SavedDataType<>(
            ID, OxygenFieldManager::new, codec());

    /** Live concentration field: world-packed BlockPos → concentration byte (absent = 0 = vacuum). */
    private final Long2ByteOpenHashMap field = new Long2ByteOpenHashMap();
    /** Active oxygen-source cells (world-packed), forced to MAX each step. Persisted. */
    private final LongOpenHashSet sources = new LongOpenHashSet();

    public OxygenFieldManager() {
        this.field.defaultReturnValue((byte) 0);
    }

    private static Codec<OxygenFieldManager> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                Codec.LONG.listOf().fieldOf("sources").forGetter(m -> new ArrayList<>(m.sources))
        ).apply(inst, OxygenFieldManager::fromSources));
    }

    private static OxygenFieldManager fromSources(List<Long> sources) {
        OxygenFieldManager m = new OxygenFieldManager();
        for (long s : sources) {
            m.sources.add(s);
            m.field.put(s, (byte) Config.OXYGEN_MAX_CONCENTRATION.get().intValue());
        }
        return m;
    }

    public static OxygenFieldManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // --- Source registry ----------------------------------------------------

    public void addSource(BlockPos pos) {
        long key = pos.asLong();
        if (this.sources.add(key)) {
            setDirty(); // injection happens at the source's air neighbours during simulate()
        }
    }

    public void removeSource(BlockPos pos) {
        if (this.sources.remove(pos.asLong())) {
            setDirty();
        }
    }

    public boolean isSource(BlockPos pos) {
        return this.sources.contains(pos.asLong());
    }

    // --- Lookup (O(1)) ------------------------------------------------------

    /** @return concentration {@code 0..MAX} at {@code pos}. */
    public int concentrationAt(BlockPos pos) {
        return this.field.get(pos.asLong()) & 0xFF;
    }

    public boolean isBreathable(BlockPos pos) {
        return concentrationAt(pos) >= Config.OXYGEN_BREATHABLE_THRESHOLD.get();
    }

    public LongSet sourceCells() {
        return this.sources;
    }

    public int activeCellCount() {
        return this.field.size();
    }

    // --- Simulation ---------------------------------------------------------

    /** Sim-pass counter, used to pace the slow evaporation drain (not persisted). */
    private transient int simCounter;

    /**
     * One simulation pass (terraform design §1, reworked for gas-like behaviour). Each pass:
     *
     * <ol>
     *   <li><b>Flood from each active source</b> through the connected air space (BFS), detecting
     *       whether that volume is <i>sealed</i> (BFS never reaches a sky-exposed cell and stays under
     *       the cap) or <i>leaky/open</i> (it does). A sealed volume is the breathable target at full
     *       strength everywhere (the room fills); a leaky/open volume only pressurises a small bubble
     *       around the generator and bleeds to 0 toward the opening — oxygen finds the leak and escapes.
     *   <li><b>Ease the live field toward that target:</b> rise to it immediately (fill), but drain
     *       toward it slowly so that losing supply (out of fuel / broken generator) or springing a leak
     *       makes the oxygen evaporate over {@code oxygenEvaporateSeconds} rather than vanishing.</li>
     * </ol>
     *
     * <p>Because the target is recomputed every pass from the current blocks, the field re-paths
     * automatically when the surroundings change — seal a wall and the room fills; break one and it
     * leaks out.</p>
     */
    public void simulate(ServerLevel level) {
        if (this.sources.isEmpty() && this.field.isEmpty()) {
            return;
        }
        // Run while a player is near a source, or while leftover oxygen still needs to evaporate.
        boolean run = anyPlayerNearSource(level) || (!this.field.isEmpty() && !level.players().isEmpty());
        if (!run) {
            return; // paused; persisted state resumes when a player returns
        }

        this.simCounter++;
        final int max = Config.OXYGEN_MAX_CONCENTRATION.get();
        final int cap = Config.OXYGEN_MAX_ACTIVE_CELLS_PER_SOURCE.get();
        final int bubbleR = Math.max(1, Config.OXYGEN_BUBBLE_RADIUS.get());
        final int leakRange = Config.OXYGEN_LEAK_RANGE.get();
        final int interval = Config.OXYGEN_SIM_INTERVAL_TICKS.get();
        // Drain one concentration level every N passes so a full cell reaches 0 in ~evaporateSeconds.
        int passesToEmpty = Math.max(1, Math.round(Config.OXYGEN_EVAPORATE_SECONDS.get() * 20.0F / interval));
        int drainEvery = Math.max(1, Math.round((float) passesToEmpty / max));
        boolean drainTick = this.simCounter % drainEvery == 0;

        // 1. Target field: flood-fill from every active source.
        Long2ByteOpenHashMap target = new Long2ByteOpenHashMap();
        target.defaultReturnValue((byte) 0);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        LongIterator si = this.sources.iterator();
        while (si.hasNext()) {
            floodFromSource(level, BlockPos.of(si.nextLong()), max, cap, bubbleR, leakRange, target, m);
        }

        // 2. Ease the live field toward the target: snap up to fill, drain slowly to evaporate/leak.
        LongOpenHashSet cells = new LongOpenHashSet(this.field.keySet());
        cells.addAll(target.keySet());
        Long2ByteOpenHashMap next = new Long2ByteOpenHashMap();
        next.defaultReturnValue((byte) 0);
        LongIterator ci = cells.iterator();
        while (ci.hasNext()) {
            long c = ci.nextLong();
            int old = this.field.get(c) & 0xFF;
            int tgt = target.get(c) & 0xFF;
            int val;
            if (tgt >= old) {
                val = tgt;                                  // fill: rise to target immediately
            } else if (drainTick) {
                val = Math.max(tgt, old - 1);               // evaporate/leak: drain one level per drain tick
            } else {
                val = old;
            }
            if (val > 0) {
                next.put(c, (byte) val);
            }
        }
        this.field.clear();
        this.field.putAll(next);

        if (Config.OXYGEN_DEBUG_LOG.get()) {
            Nerospace.LOGGER.info("[oxygen] dim={} sources={} activeCells={}",
                    level.dimension(), this.sources.size(), this.field.size());
        }
    }

    /**
     * BFS the connected air space from a source block and write its breathable target into {@code out}.
     * Sealed volumes (no sky-exposed cell, under the cap) fill to MAX everywhere; leaky/open volumes
     * pressurise only a {@code bubbleR} falloff around the source and drop to 0 toward the opening.
     */
    private void floodFromSource(ServerLevel level, BlockPos source, int max, int cap, int bubbleR,
                                 int leakRange, Long2ByteOpenHashMap out, BlockPos.MutableBlockPos m) {
        Long2IntOpenHashMap dist = new Long2IntOpenHashMap();
        dist.defaultReturnValue(-1);
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        // Seed from the source's holdable air neighbours (the generator block itself is solid).
        for (Direction dir : Direction.values()) {
            m.setWithOffset(source, dir);
            if (level.hasChunkAt(m) && OxygenField.canHold(level, m, level.getBlockState(m))) {
                long k = m.asLong();
                if (dist.get(k) < 0) {
                    dist.put(k, 0);
                    queue.enqueue(k);
                }
            }
        }
        if (queue.isEmpty()) {
            return;
        }

        boolean leaked = false;
        boolean capped = false;
        BlockPos.MutableBlockPos mm = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            if (dist.size() > cap) {
                capped = true; // too big to confirm sealed → treat as open
                break;
            }
            long c = queue.dequeueLong();
            int d = dist.get(c);
            BlockPos cp = BlockPos.of(c);
            if (level.canSeeSky(cp)) {
                leaked = true; // an opening to the sky/vacuum — the volume is not sealed
            }
            if (d >= leakRange) {
                continue; // a generator only searches/pressurises out to oxygenLeakRange blocks
            }
            for (Direction dir : Direction.values()) {
                mm.setWithOffset(cp, dir);
                if (!level.hasChunkAt(mm)) {
                    continue;
                }
                long nk = mm.asLong();
                if (dist.get(nk) >= 0) {
                    continue;
                }
                if (OxygenField.canHold(level, mm, level.getBlockState(mm))) {
                    dist.put(nk, d + 1);
                    queue.enqueue(nk);
                }
            }
        }

        boolean sealed = !leaked && !capped;
        for (Long2IntMap.Entry e : dist.long2IntEntrySet()) {
            int d = e.getIntValue();
            int val = sealed ? max : Math.max(0, Math.round(max * (1.0F - (float) d / bubbleR)));
            if (val <= 0) {
                continue;
            }
            long k = e.getLongKey();
            if ((out.get(k) & 0xFF) < val) {
                out.put(k, (byte) val);
            }
        }
    }

    private boolean anyPlayerNearSource(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return false;
        }
        double rSq = (double) Config.OXYGEN_SYNC_RADIUS.get() * Config.OXYGEN_SYNC_RADIUS.get();
        LongIterator si = this.sources.iterator();
        while (si.hasNext()) {
            BlockPos sp = BlockPos.of(si.nextLong());
            for (ServerPlayer p : players) {
                if (p.distanceToSqr(sp.getX() + 0.5, sp.getY() + 0.5, sp.getZ() + 0.5) <= rSq) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Snapshot of cells within {@code radius} of {@code center} for the client sync packet (§1.7). */
    public Long2ByteMap snapshotAround(BlockPos center, int radius) {
        Long2ByteOpenHashMap out = new Long2ByteOpenHashMap();
        long rSq = (long) radius * radius;
        for (Long2ByteMap.Entry e : this.field.long2ByteEntrySet()) {
            BlockPos p = BlockPos.of(e.getLongKey());
            if (center.distSqr(p) <= rSq) {
                out.put(e.getLongKey(), e.getByteValue());
            }
        }
        return out;
    }
}
