package za.co.neroland.nerospace.block.rules;

/** Dawnband Stone: during a Flare it glows 11 and shields COLD within 4 blocks; idle light 2. */
public final class WarmthRules {

    public static final double RADIUS = 4.0;
    public static final int LIGHT_IDLE = 2;
    public static final int LIGHT_FLARING = 11;
    /** Blocks flipped per chunk per flare transition (config may lower). */
    public static final int PER_CHUNK_CAP = 256;

    private WarmthRules() {
    }

    public static boolean shielded(boolean flaring, double distance, double radius) {
        return flaring && distance >= 0.0 && distance <= Math.min(radius, RADIUS);
    }

    public static int light(boolean flaring) {
        return flaring ? LIGHT_FLARING : LIGHT_IDLE;
    }
}
