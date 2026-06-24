package za.co.neroland.nerospace.village;

/**
 * Reputation tier math for the Alien Villagers (ALIEN_VILLAGERS_DESIGN.md §3). A per-player score in
 * {@code [0, MAX]} maps to <b>6 tiers</b> (T0 Stranger → T5 Kin). Tiers gate trades now, and will gate
 * teaching/structure access in later phases.
 *
 * <p>Interim note (Phase 2): reputation is stored per-villager (on the entity). When the Village Core
 * block arrives (Phase 3/4) it will aggregate reputation per-village, as the design specifies.
 */
public final class Reputation {

    public static final int MAX = 100;

    /** Minimum score for each tier T0..T5. */
    private static final int[] TIER_MIN = {0, 10, 25, 45, 70, 95};

    public static final int MAX_TIER = TIER_MIN.length - 1;

    private Reputation() {
    }

    /** The tier (0..5) for a raw reputation score. */
    public static int tier(int score) {
        int t = 0;
        for (int i = 0; i < TIER_MIN.length; i++) {
            if (score >= TIER_MIN[i]) {
                t = i;
            }
        }
        return t;
    }

    public static int clamp(int score) {
        return Math.max(0, Math.min(MAX, score));
    }
}
