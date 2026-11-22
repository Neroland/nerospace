package za.co.neroland.nerospace.suit;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Widens vanilla's movement checks only while suit flight or EVA is active, and only up to the suit's own
 * speed cap. Off by default. The map is weakly keyed by the player object and holds no identifier.
 */
public final class MovementAllowance {

    public record Grant(double maxSpeedBlocksPerTick, boolean eva) {
    }

    private static final Map<Object, Grant> ACTIVE = new WeakHashMap<>();

    private MovementAllowance() {
    }

    public static void activate(Object player, double maxSpeed, boolean eva) {
        if (player == null) {
            return;
        }
        ACTIVE.put(player, new Grant(Math.max(0.0D, maxSpeed), eva));
    }

    public static void clear(Object player) {
        if (player != null) {
            ACTIVE.remove(player);
        }
    }

    public static boolean active(Object player) {
        return player != null && ACTIVE.containsKey(player);
    }

    public static Grant grant(Object player) {
        return player == null ? null : ACTIVE.get(player);
    }

    /** True when a movement of {@code blocks} this tick is inside the grant. No grant → never allowed. */
    public static boolean withinCap(Object player, double blocks) {
        Grant grant = grant(player);
        if (grant == null) {
            return false;
        }
        return Math.abs(blocks) <= grant.maxSpeedBlocksPerTick() + 1.0E-4;
    }

    /** Test hook. */
    public static void clearAll() {
        ACTIVE.clear();
    }
}
