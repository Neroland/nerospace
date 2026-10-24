package za.co.neroland.nerospace.block;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * Bounded "loaded chunks" iteration for weather-driven block effects. Common code has no loaded-chunk iterator, so
 * this walks the chunks within {@link #RADIUS} of each player (de-duplicated) and skips any not already loaded. With
 * the default radius that is ≤ 81 chunks per player — the 002 §3 budget of one action per chunk per cadence.
 * No player data is kept.
 */
public final class LoadedChunks {

    /** Chunk radius around each player (9×9). */
    public static final int RADIUS = 4;

    private LoadedChunks() {
    }

    @FunctionalInterface
    public interface ChunkVisitor {
        void visit(int chunkX, int chunkZ);
    }

    public static void forEachNearPlayers(ServerLevel level, ChunkVisitor visitor) {
        if (level.players().isEmpty()) {
            return;
        }
        LongOpenHashSet seen = new LongOpenHashSet();
        for (ServerPlayer player : level.players()) {
            int pcx = player.getBlockX() >> 4;
            int pcz = player.getBlockZ() >> 4;
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    int cx = pcx + dx;
                    int cz = pcz + dz;
                    if (seen.add(ChunkPos.pack(cx, cz)) && level.getChunkSource().getChunkNow(cx, cz) != null) {
                        visitor.visit(cx, cz);
                    }
                }
            }
        }
    }
}
