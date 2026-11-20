package za.co.neroland.nerospace.rocket;

/**
 * Pure numbers for the one-off flame and smoke burst at the engine bells on the tick a rocket lifts off.
 * Client-side and cosmetic: the particles go through the normal particle path, so the player's vanilla
 * particle setting thins or drops them, and the counts here are the hard upper bound per launch.
 */
public final class IgnitionBurst {

    /** No launch ever spawns more flame particles than this, whatever the tier. */
    public static final int MAX_FLAMES = 72;

    private IgnitionBurst() {
    }

    /** Flame particles for a rocket of {@code tierLevel} (1..4): 36, 48, 60, 72. */
    public static int flames(int tierLevel) {
        int level = Math.max(1, Math.min(4, tierLevel));
        return Math.min(MAX_FLAMES, 24 + 12 * level);
    }

    /** Smoke particles rolling out with the flames: half as many. */
    public static int smoke(int tierLevel) {
        return flames(tierLevel) / 2;
    }

    /** Outward speed of the burst in blocks per tick, a little wider for a bigger hull. */
    public static double spread(int tierLevel) {
        int level = Math.max(1, Math.min(4, tierLevel));
        return 0.16D + 0.04D * level;
    }
}
