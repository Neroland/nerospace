package za.co.neroland.nerospace.block.rules;

/**
 * Brine: a fluid-like block. An entity inside is slowed, bobs upward a little, has its fall reset and is put out if it
 * burns. Brine is not water: it never freezes and never flows.
 */
public final class BrineRules {

    /** Horizontal and vertical velocity multiplier per tick inside brine. */
    public static final double DRAG = 0.7;
    /** Largest upward push per tick. */
    public static final double MAX_LIFT = 0.04;

    private BrineRules() {
    }

    public static double drag() {
        return DRAG;
    }

    /** Upward push per tick for how much of the entity is under the surface (0..1), capped at {@link #MAX_LIFT}. */
    public static double buoyancy(double submergedFraction) {
        double f = Math.max(0.0, Math.min(1.0, submergedFraction));
        return Math.min(MAX_LIFT, f * MAX_LIFT);
    }

    /** How much of an entity of the given height is below the brine surface. */
    public static double submergedFraction(double entityFeetY, double entityHeight, double surfaceY) {
        if (entityHeight <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (surfaceY - entityFeetY) / entityHeight));
    }

    public static boolean freezes() {
        return false;
    }

    public static boolean extinguishes() {
        return true;
    }

    public static boolean resetsFall() {
        return true;
    }
}
