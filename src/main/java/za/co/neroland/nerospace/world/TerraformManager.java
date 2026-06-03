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
import za.co.neroland.nerospace.registry.ModAttachments;

/**
 * Per-{@link ServerLevel} registry of active Terraformers and how far each has expanded (terraform
 * design §2.3, lazy chunk handling). The live frontier skips columns whose chunk is unloaded; this
 * registry lets a chunk-load handler convert any in-range columns when those chunks load later, so a
 * terraformed planet finishes converting as the player explores it — without force-loading.
 *
 * <p>Persists each terraformer's {@code (centre, radius, tier)} so the catch-up works even while the
 * terraformer's own chunk is unloaded.</p>
 */
public final class TerraformManager extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Nerospace.MODID, "terraformers");

    public static final SavedDataType<TerraformManager> TYPE = new SavedDataType<>(
            ID, TerraformManager::new, codec());

    /** Terraformer centre (packed BlockPos) → current horizontal radius. */
    private final Long2IntOpenHashMap radius = new Long2IntOpenHashMap();
    /** Terraformer centre (packed BlockPos) → machine tier. */
    private final Long2IntOpenHashMap tier = new Long2IntOpenHashMap();

    public TerraformManager() {
        this.radius.defaultReturnValue(-1);
        this.tier.defaultReturnValue(1);
    }

    private static Codec<TerraformManager> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                Codec.LONG.listOf().fieldOf("positions").forGetter(m -> new ArrayList<>(m.radius.keySet())),
                Codec.INT.listOf().fieldOf("radii").forGetter(TerraformManager::radiiInKeyOrder),
                Codec.INT.listOf().fieldOf("tiers").forGetter(TerraformManager::tiersInKeyOrder)
        ).apply(inst, TerraformManager::fromLists));
    }

    private List<Integer> radiiInKeyOrder() {
        List<Integer> out = new ArrayList<>();
        for (long k : this.radius.keySet()) {
            out.add(this.radius.get(k));
        }
        return out;
    }

    private List<Integer> tiersInKeyOrder() {
        List<Integer> out = new ArrayList<>();
        for (long k : this.radius.keySet()) {
            out.add(this.tier.getOrDefault(k, 1));
        }
        return out;
    }

    private static TerraformManager fromLists(List<Long> positions, List<Integer> radii, List<Integer> tiers) {
        TerraformManager m = new TerraformManager();
        for (int i = 0; i < positions.size(); i++) {
            long key = positions.get(i);
            m.radius.put(key, i < radii.size() ? radii.get(i) : 0);
            m.tier.put(key, i < tiers.size() ? tiers.get(i) : 1);
        }
        return m;
    }

    public static TerraformManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** A terraformer reports its current reach each work cycle. */
    public void update(BlockPos center, int currentRadius, int machineTier) {
        long key = center.asLong();
        boolean changed = this.radius.get(key) != currentRadius || this.tier.get(key) != machineTier;
        if (changed) {
            this.radius.put(key, currentRadius);
            this.tier.put(key, machineTier);
            setDirty();
        }
    }

    public void remove(BlockPos center) {
        long key = center.asLong();
        if (this.radius.remove(key) != this.radius.defaultReturnValue()) {
            this.tier.remove(key);
            setDirty();
        }
    }

    /**
     * Catch-up conversion when a chunk loads: convert any column in {@code chunk} that lies within an
     * active terraformer's radius and hasn't been terraformed yet.
     */
    public void onChunkLoaded(ServerLevel level, LevelChunk chunk) {
        if (this.radius.isEmpty()) {
            return;
        }
        // Already-terraformed chunks are done (the flag is set once any column converts).
        if (Boolean.TRUE.equals(chunk.getData(ModAttachments.TERRAFORMED))) {
            return;
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
            long rSq = (long) r * r;
            int t = this.tier.getOrDefault(e.getLongKey(), 1);
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int x = minX + dx;
                    int z = minZ + dz;
                    long ddx = x - cx;
                    long ddz = z - cz;
                    if (ddx * ddx + ddz * ddz <= rSq) {
                        TerraformConversion.convertColumn(level, x, z, t, biomeChanged);
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
