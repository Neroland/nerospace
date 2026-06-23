package za.co.neroland.nerospace.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.TerraformConversion;

/**
 * Per-{@link ServerLevel} registry of active Terraformers and how far each has expanded (terraform
 * design §2.3, lazy chunk handling; staged per DEEPER_TERRAFORM_DESIGN.md). The live frontier skips
 * columns whose chunk is unloaded; this registry lets a chunk-load handler convert any in-range
 * columns when those chunks load later, so a terraformed planet finishes converting as the player
 * explores it — without force-loading.
 *
 * <p>Persists each terraformer's {@code (centre, radius, hydrationRadius, lifeRadius, tier)} so the
 * catch-up works even while the terraformer's own chunk is unloaded. The stage radii are OPTIONAL in
 * the codec (default 0) so pre-stage saves parse unchanged — the no-break contract (§9). Catch-up
 * replays every stage whose radius reaches the chunk, water included, for free: exactly the trade the
 * stage-1 catch-up has always made (energy/glacite throttle the radius only while chunks are
 * loaded).</p>
 */
public final class TerraformManager extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Nerospace.MODID, "terraformers");

    public static final SavedDataType<TerraformManager> TYPE = new SavedDataType<>(
            ID, TerraformManager::new, codec());

    /** Terraformer centre (packed BlockPos) → current horizontal stage-1 radius. */
    private final Long2IntOpenHashMap radius = new Long2IntOpenHashMap();
    /** Terraformer centre (packed BlockPos) → machine tier. */
    private final Long2IntOpenHashMap tier = new Long2IntOpenHashMap();
    /** Terraformer centre (packed BlockPos) → stage-2 (Hydrated) radius. */
    private final Long2IntOpenHashMap hydrationRadius = new Long2IntOpenHashMap();
    /** Terraformer centre (packed BlockPos) → stage-3 (Living) radius. */
    private final Long2IntOpenHashMap lifeRadius = new Long2IntOpenHashMap();

    public TerraformManager() {
        this.radius.defaultReturnValue(-1);
        this.tier.defaultReturnValue(1);
        this.hydrationRadius.defaultReturnValue(0);
        this.lifeRadius.defaultReturnValue(0);
    }

    /** Public for the save-compat gametest, which decodes a legacy (pre-stage) payload through it. */
    public static Codec<TerraformManager> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                Codec.LONG.listOf().fieldOf("positions").forGetter(m -> new ArrayList<>(m.radius.keySet())),
                Codec.INT.listOf().fieldOf("radii").forGetter(m -> m.inKeyOrder(m.radius, 0)),
                Codec.INT.listOf().fieldOf("tiers").forGetter(m -> m.inKeyOrder(m.tier, 1)),
                // Stage radii are additive (DEEPER_TERRAFORM_DESIGN.md §9): absent in legacy saves.
                Codec.INT.listOf().optionalFieldOf("hydration_radii", List.of())
                        .forGetter(m -> m.inKeyOrder(m.hydrationRadius, 0)),
                Codec.INT.listOf().optionalFieldOf("life_radii", List.of())
                        .forGetter(m -> m.inKeyOrder(m.lifeRadius, 0))
        ).apply(inst, TerraformManager::fromLists));
    }

    /** The map's values in {@link #radius} key order (the codec's shared ordering), with a default. */
    private List<Integer> inKeyOrder(Long2IntOpenHashMap map, int fallback) {
        List<Integer> out = new ArrayList<>();
        for (long k : this.radius.keySet()) {
            out.add(map.containsKey(k) ? map.get(k) : fallback);
        }
        return out;
    }

    private static TerraformManager fromLists(List<Long> positions, List<Integer> radii,
            List<Integer> tiers, List<Integer> hydrationRadii, List<Integer> lifeRadii) {
        TerraformManager m = new TerraformManager();
        for (int i = 0; i < positions.size(); i++) {
            long key = positions.get(i);
            m.radius.put(key, i < radii.size() ? radii.get(i) : 0);
            m.tier.put(key, i < tiers.size() ? tiers.get(i) : 1);
            m.hydrationRadius.put(key, i < hydrationRadii.size() ? hydrationRadii.get(i) : 0);
            m.lifeRadius.put(key, i < lifeRadii.size() ? lifeRadii.get(i) : 0);
        }
        return m;
    }

    public static TerraformManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Visits every registered terraformer with its centre and per-stage radii. */
    public interface MachineVisitor {
        void visit(BlockPos center, int radius, int hydrationRadius, int lifeRadius);
    }

    /** Iterates the registered machines (cosmetic drift target selection — design §2.3). */
    public void forEachMachine(MachineVisitor visitor) {
        for (Long2IntMap.Entry e : this.radius.long2IntEntrySet()) {
            long key = e.getLongKey();
            visitor.visit(BlockPos.of(key), Math.max(0, e.getIntValue()),
                    this.hydrationRadius.get(key), this.lifeRadius.get(key));
        }
    }

    /**
     * The recorded radius of {@code center}'s stage frontier (1 = Rooted, 2 = Hydrated, 3 = Living);
     * 0 for unknown machines. Used by the Terraform Monitor readout and the save-compat gametest.
     */
    public int stageRadius(BlockPos center, int stage) {
        long key = center.asLong();
        return switch (stage) {
            case 2 -> this.hydrationRadius.get(key);
            case 3 -> this.lifeRadius.get(key);
            default -> Math.max(0, this.radius.get(key));
        };
    }

    /** A terraformer reports its current reach (all stage frontiers) each work cycle. */
    public void update(BlockPos center, int currentRadius, int currentHydrationRadius,
            int currentLifeRadius, int machineTier) {
        long key = center.asLong();
        boolean changed = this.radius.get(key) != currentRadius
                || this.tier.get(key) != machineTier
                || this.hydrationRadius.get(key) != currentHydrationRadius
                || this.lifeRadius.get(key) != currentLifeRadius;
        if (changed) {
            this.radius.put(key, currentRadius);
            this.tier.put(key, machineTier);
            this.hydrationRadius.put(key, currentHydrationRadius);
            this.lifeRadius.put(key, currentLifeRadius);
            setDirty();
        }
    }

    public void remove(BlockPos center) {
        long key = center.asLong();
        if (this.radius.remove(key) != this.radius.defaultReturnValue()) {
            this.tier.remove(key);
            this.hydrationRadius.remove(key);
            this.lifeRadius.remove(key);
            setDirty();
        }
    }

    /**
     * Catch-up conversion when a chunk loads: replay, per column, every stage whose radius reaches it
     * and that the chunk hasn't recorded yet. Stage replays above the chunk's recorded stage only —
     * stage-1 work (plant scatter) is not strictly idempotent across repeats, so the recorded stage is
     * the skip line, exactly as the {@code TERRAFORMED} flag was before stages existed.
     */
    public void onChunkLoaded(ServerLevel level, LevelChunk chunk) {
        if (this.radius.isEmpty()) {
            return;
        }
        int chunkStage = TerraformConversion.effectiveStage(chunk);
        if (chunkStage >= 3) {
            return; // fully Living — nothing left to replay
        }
        ChunkPos cp = chunk.getPos();
        int minX = cp.getMinBlockX();
        int minZ = cp.getMinBlockZ();
        Set<LevelChunk> biomeChanged = new HashSet<>();
        boolean any = false;

        for (Long2IntMap.Entry e : this.radius.long2IntEntrySet()) {
            BlockPos center = BlockPos.of(e.getLongKey());
            int r = e.getIntValue();
            if (r <= 0) {
                continue;
            }
            // Skip terraformers whose radius can't reach this chunk at all.
            int cx = center.getX();
            int cz = center.getZ();
            if (cx + r < minX || cx - r > minX + 15 || cz + r < minZ || cz - r > minZ + 15) {
                continue;
            }
            long key = e.getLongKey();
            long rSq = (long) r * r;
            int hydR = this.hydrationRadius.get(key);
            long hydSq = (long) hydR * hydR;
            int lifeR = this.lifeRadius.get(key);
            long lifeSq = (long) lifeR * lifeR;
            int t = this.tier.getOrDefault(key, 1);
            // The catch-up water table mirrors TerraformerBlockEntity#waterTableY (machine base − 1).
            int tableY = center.getY() - 1;

            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int x = minX + dx;
                    int z = minZ + dz;
                    long ddx = x - cx;
                    long ddz = z - cz;
                    long dSq = ddx * ddx + ddz * ddz;
                    if (dSq > rSq) {
                        continue;
                    }
                    if (chunkStage < 1) {
                        TerraformConversion.convertColumn(level, x, z, t, biomeChanged);
                        any = true;
                    }
                    if (chunkStage < 2 && hydR > 0 && dSq <= hydSq) {
                        TerraformConversion.hydrateColumn(level, x, z, tableY, null);
                        any = true;
                    }
                    if (chunkStage < 3 && lifeR > 0 && dSq <= lifeSq) {
                        TerraformConversion.vivifyColumn(level, x, z, biomeChanged);
                        any = true;
                    }
                }
            }
        }

        if (any && !biomeChanged.isEmpty()) {
            ClientboundChunksBiomesPacket packet =
                    ClientboundChunksBiomesPacket.forChunks(new ArrayList<>(biomeChanged));
            for (ServerPlayer player : level.players()) {
                player.connection.send(packet);
            }
        }
    }
}
