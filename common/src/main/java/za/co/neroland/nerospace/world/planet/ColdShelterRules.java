package za.co.neroland.nerospace.world.planet;

/**
 * Pure half of {@link ColdShelter}: which world's positional cold shield answers for a planet, and when a cached
 * answer may be reused.
 */
public final class ColdShelterRules {

    /** Game ticks one cached answer stays valid for. */
    public static final int BUCKET_TICKS = 10;

    /** Whose lookup answers for a planet. */
    public enum Source {
        /** No positional cold shield on this world. */
        NONE,
        /** A flaring Dawnband Stone. */
        NYXARA,
        /** An active hydrothermal vent or warm basalt. */
        EUROPA
    }

    private ColdShelterRules() {
    }

    /** The lookup for a planet id ({@code nerospace:<name>}); {@link Source#NONE} for every other world. */
    public static Source source(String planetId) {
        if ("nerospace:nyxara".equals(planetId)) {
            return Source.NYXARA;
        }
        if ("nerospace:europa".equals(planetId)) {
            return Source.EUROPA;
        }
        return Source.NONE;
    }

    /** The cache bucket a game time falls in. */
    public static long bucket(long gameTime) {
        return Math.floorDiv(gameTime, (long) BUCKET_TICKS);
    }

    /** Whether a cached answer for ({@code cachedPos}, {@code cachedBucket}) serves a call for the same pair. */
    public static boolean cacheHit(boolean hasEntry, long cachedPos, long cachedBucket, long pos, long bucket) {
        return hasEntry && cachedPos == pos && cachedBucket == bucket;
    }
}
