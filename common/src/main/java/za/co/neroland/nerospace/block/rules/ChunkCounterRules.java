package za.co.neroland.nerospace.block.rules;

/** Cache policy for per-chunk spread counts: a cached count is trusted for 200 ticks, then rescanned. */
public final class ChunkCounterRules {

    public static final int TTL_TICKS = 200;

    private ChunkCounterRules() {
    }

    public static boolean stale(long cachedAt, long now) {
        return cachedAt < 0 || now - cachedAt >= TTL_TICKS || now < cachedAt;
    }
}
