package za.co.neroland.nerospace.suit;

/** Virtual durability. The item's real max is far higher so a hit can never break it; breach is one point left. */
public final class SuitDurability {

    /** Real item max, high enough that one hit cannot cross it. The bar uses {@link #max}. */
    public static final int ITEM_MAX = 100_000;

    private SuitDurability() {
    }

    public static int max(SuitPiece piece, SuitFrame frame) {
        return frame.durabilityMultiplier() * piece.durabilityFactor;
    }

    /** Damage is clamped to one below the virtual max. That last point is the breached state. */
    public static int clamp(int damage, int virtualMax) {
        if (virtualMax <= 1) {
            return 0;
        }
        return Math.max(0, Math.min(damage, virtualMax - 1));
    }

    public static boolean breached(int damage, int virtualMax) {
        return damage >= virtualMax - 1;
    }
}
