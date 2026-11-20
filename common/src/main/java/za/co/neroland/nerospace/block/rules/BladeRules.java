package za.co.neroland.nerospace.block.rules;

/**
 * Penitente Blade: landing on a blade from 2 blocks or more hurts half as much again and snaps the blade. Shorter
 * drops do nothing special, and native fauna are never hurt by it.
 */
public final class BladeRules {

    /** Shortest fall that triggers the blade. */
    public static final double MIN_FALL = 2.0;
    /** Fall-damage multiplier for a triggering fall. */
    public static final float DAMAGE_FACTOR = 1.5F;

    private BladeRules() {
    }

    public static boolean breaks(double fallDistance) {
        return fallDistance >= MIN_FALL;
    }

    /** The fall-damage multiplier for this fall: 1.5 from {@link #MIN_FALL}, 1 below, 0 for natives. */
    public static float damageFactor(double fallDistance, boolean isNative) {
        if (isNative) {
            return 0.0F;
        }
        return breaks(fallDistance) ? DAMAGE_FACTOR : 1.0F;
    }

    /** Damage dealt for a base fall damage. */
    public static float damage(double fallDistance, float base) {
        return base * damageFactor(fallDistance, false);
    }
}
