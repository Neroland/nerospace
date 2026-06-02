package za.co.neroland.nerospace.world;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
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

    /** One relaxation pass. Skipped (paused) when no player is near any source. */
    public void simulate(ServerLevel level) {
        if (this.sources.isEmpty() && this.field.isEmpty()) {
            return;
        }
        if (!anyPlayerNearSource(level)) {
            return; // paused; persisted state resumes correctly when a player returns
        }

        final int max = Config.OXYGEN_MAX_CONCENTRATION.get();
        final double diffusion = Config.OXYGEN_DIFFUSION_RATE.get();
        final double decay = Config.OXYGEN_DECAY_PER_STEP.get();
        final int cap = Config.OXYGEN_MAX_ACTIVE_CELLS_PER_SOURCE.get()
                * Math.max(1, this.sources.size());

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        // Injection cells: the holdable cells adjacent to each source block (a generator is a solid
        // cube, so it can't hold oxygen itself — it pumps into the air around it). These are clamped
        // to MAX each step and are the field's entry points.
        LongOpenHashSet inject = new LongOpenHashSet();
        LongIterator si = this.sources.iterator();
        while (si.hasNext()) {
            BlockPos sp = BlockPos.of(si.nextLong());
            for (Direction dir : Direction.values()) {
                m.setWithOffset(sp, dir);
                if (level.hasChunkAt(m) && OxygenField.canHold(level, m, level.getBlockState(m))) {
                    inject.add(m.asLong());
                }
            }
        }

        // Build the active set: current field + injection cells + their passable neighbours (the
        // frontier grows one ring/step). Bounded by the safety cap so an open-vacuum dump just stops.
        LongOpenHashSet active = new LongOpenHashSet(this.field.keySet());
        active.addAll(inject);
        LongOpenHashSet frontier = new LongOpenHashSet(active);
        LongIterator it = frontier.iterator();
        while (it.hasNext()) {
            long c = it.nextLong();
            BlockPos cp = BlockPos.of(c);
            for (Direction dir : Direction.values()) {
                m.setWithOffset(cp, dir);
                if (active.size() >= cap) {
                    break;
                }
                long np = m.asLong();
                if (!active.contains(np) && level.hasChunkAt(m)
                        && OxygenField.canHold(level, m, level.getBlockState(m))) {
                    active.add(np);
                }
            }
        }

        Long2ByteOpenHashMap next = new Long2ByteOpenHashMap();
        next.defaultReturnValue((byte) 0);

        LongIterator ai = active.iterator();
        while (ai.hasNext()) {
            long c = ai.nextLong();
            BlockPos cp = BlockPos.of(c);
            if (!level.hasChunkAt(cp)) {
                continue;
            }
            var state = level.getBlockState(cp);
            if (!OxygenField.canHold(level, cp, state)) {
                continue; // sealed/solid cells never hold oxygen
            }
            if (inject.contains(c)) {
                next.put(c, (byte) max); // injection clamp at the source's air cells
                continue;
            }
            int oldC = this.field.get(c) & 0xFF;
            double sum = 0.0D;
            for (Direction dir : Direction.values()) {
                m.setWithOffset(cp, dir);
                if (!level.hasChunkAt(m)) {
                    continue;
                }
                if (OxygenField.canHold(level, m, level.getBlockState(m))) {
                    sum += (this.field.get(m.asLong()) & 0xFF) - oldC;
                }
            }
            // Decay is the bleed to vacuum: full strength only where oxygen can actually escape — a
            // cell open to the sky or a leaky block. Sealed interior cells (roof/walls above) barely
            // bleed, so an enclosed room fills to near-max, while an open-air bubble stays small and a
            // hole/open door leaks (its outside cells see sky and decay). This is what makes a sealed
            // base breathable across the whole room instead of just a few blocks around the generator.
            boolean exposed = level.canSeeSky(cp) || OxygenField.isLeaky(level, cp, state);
            double localDecay = exposed ? decay : decay * 0.12D;
            double newF = oldC + diffusion * sum - localDecay;
            int newV = (int) Math.round(Math.max(0.0D, Math.min(max, newF)));
            if (newV > 0) {
                next.put(c, (byte) newV);
            }
        }

        this.field.clear();
        this.field.putAll(next);

        if (Config.OXYGEN_DEBUG_LOG.get()) {
            Nerospace.LOGGER.info("[oxygen] dim={} sources={} activeCells={}",
                    level.dimension(), this.sources.size(), this.field.size());
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
