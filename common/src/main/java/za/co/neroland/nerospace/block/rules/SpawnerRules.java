package za.co.neroland.nerospace.block.rules;

/** Dust Devil Pit (002 §1.2): during a storm, 1-in-3 per 600 ticks, at most 2 devils in the 3×3 chunk area. */
public final class SpawnerRules {

    public static final int INTERVAL_TICKS = 600;
    public static final int ONE_IN = 3;
    public static final int MAX_NEARBY = 2;
    public static final double EXCLUSION_RADIUS = 32.0;
    public static final int LIFETIME_TICKS = 600;
    public static final double TOSS_RADIUS = 2.0;
    public static final double TOSS_UP = 0.4;
    public static final double TOSS_SIDE = 0.2;

    private SpawnerRules() {
    }

    public static boolean canSpawn(boolean storm, int nearbyDevils, int maxNearby, boolean rolled) {
        return storm && rolled && nearbyDevils < Math.min(maxNearby, MAX_NEARBY);
    }
}
