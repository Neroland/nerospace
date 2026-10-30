package za.co.neroland.nerospace.block.rules;

/** Ember-Cracked Ashstone: 1.0 fire-type damage every 20 ticks without heat boots. */
public final class HeatContactRules {

    public static final float DAMAGE = 1.0F;
    public static final int INTERVAL_TICKS = 20;

    private HeatContactRules() {
    }

    public static float damage(boolean livingEntity, boolean heatBoots) {
        return livingEntity && !heatBoots ? DAMAGE : 0.0F;
    }

    /** Throttle: damage only on ticks where {@code gameTime % 20 == 0}. */
    public static boolean dueThisTick(long gameTime) {
        return Math.floorMod(gameTime, (long) INTERVAL_TICKS) == 0;
    }
}
