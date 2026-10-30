package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;

/**
 * The chunk budget of one landing, pure. The Landing Pod search reads only the 4×4-chunk square around
 * {@code preferred} ({@link #podSquare}); node probes are capped at {@link #MAX_PROBE_CHUNKS} distinct chunks
 * ({@link LandingCandidates}). A pod-site cell outside the square is treated as unsuitable, never loaded.
 */
public final class LandingChunkBudget {

    /** The pod square is this many chunks on a side. */
    public static final int SQUARE_CHUNKS = 4;
    /** Node probes (explicit / return / paired / nearest) touch at most this many distinct chunks. */
    public static final int MAX_PROBE_CHUNKS = 16;

    /** A chunk-aligned square, inclusive chunk bounds. */
    public record Square(int minChunkX, int minChunkZ, int size) {

        public boolean containsBlock(int x, int z) {
            return containsChunk(x >> 4, z >> 4);
        }

        public boolean containsChunk(int cx, int cz) {
            return cx >= this.minChunkX && cx < this.minChunkX + this.size && cz >= this.minChunkZ
                    && cz < this.minChunkZ + this.size;
        }
    }

    private LandingChunkBudget() {
    }

    /**
     * The 4×4-chunk square that best centres {@code preferred}: it always covers every spiral column
     * ({@code preferred ± PodSearch.OUTER_RADIUS}).
     */
    public static Square podSquare(BlockPos preferred) {
        return new Square(start(preferred.getX()), start(preferred.getZ()), SQUARE_CHUNKS);
    }

    private static int start(int block) {
        int chunk = block >> 4;
        return chunk - ((block & 15) < 8 ? 2 : 1);
    }

    public static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }
}
