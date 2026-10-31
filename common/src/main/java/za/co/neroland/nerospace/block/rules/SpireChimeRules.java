package za.co.neroland.nerospace.block.rules;

/**
 * Crystal Spire: a hit (left-click or projectile) chimes at pitch {@code 0.5 + 0.1 x height}, where height counts the
 * spire blocks below the one struck, capped at 2.0; at most once per 10 ticks per block. Sound only.
 */
public final class SpireChimeRules {

    public static final int COOLDOWN_TICKS = 10;
    public static final float MAX_PITCH = 2.0F;
    public static final int LIGHT = 3;
    /** Blocks counted downward at most (beyond this the pitch is capped anyway). */
    public static final int MAX_COUNT = 16;

    private SpireChimeRules() {
    }

    public static float pitch(int heightInColumn) {
        return Math.min(MAX_PITCH, 0.5F + 0.1F * Math.max(0, heightInColumn));
    }

    public static boolean cooldownOk(long lastHitTick, long now) {
        return now - lastHitTick >= COOLDOWN_TICKS;
    }
}
