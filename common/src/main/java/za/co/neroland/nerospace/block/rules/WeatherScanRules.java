package za.co.neroland.nerospace.block.rules;

/**
 * Weather transition scan budget (002 §3): a transition touches at most {@code perChunkCap} blocks per chunk and is
 * spread over ticks, {@link #CHUNKS_PER_TICK} chunks per tick.
 */
public final class WeatherScanRules {

    public static final int CHUNKS_PER_TICK = 64;

    private WeatherScanRules() {
    }

    public static boolean underCap(int touchedInChunk, int perChunkCap) {
        return touchedInChunk < perChunkCap;
    }

    /** Ticks needed to process {@code loadedChunks}. */
    public static int ticksFor(int loadedChunks) {
        return Math.max(0, (loadedChunks + CHUNKS_PER_TICK - 1) / CHUNKS_PER_TICK);
    }
}
