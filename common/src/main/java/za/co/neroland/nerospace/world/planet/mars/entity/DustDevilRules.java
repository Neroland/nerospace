package za.co.neroland.nerospace.world.planet.mars.entity;

import za.co.neroland.nerospace.block.rules.SpawnerRules;

/**
 * Pure numbers for the Dust Devil (no game classes, unit-tested). Spawning is gated by
 * {@link SpawnerRules#canSpawn}; this class adds the exclusion radius, lifetime, toss and wander maths.
 */
public final class DustDevilRules {

    /** Ticks between item tosses (an item already in the air is not re-tossed). */
    public static final int TOSS_EVERY_TICKS = 10;
    /** Horizontal wander speed, blocks per tick, along the storm direction. */
    public static final double WANDER_SPEED = 0.05;
    /** Pits visited per chunk per spawn pass (worldgen places at most one per chunk). */
    public static final int PITS_PER_CHUNK = 2;

    private DustDevilRules() {
    }

    /** A pit may spawn only during a storm, on a winning roll, with no devil within 32 blocks and fewer than two in the 3x3 chunk area. */
    public static boolean maySpawn(boolean storm, int devilsWithin32, int devilsIn3x3Chunks, boolean rolled) {
        return devilsWithin32 == 0 && SpawnerRules.canSpawn(storm, devilsIn3x3Chunks, SpawnerRules.MAX_NEARBY, rolled);
    }

    /** True when a devil must leave: lifetime over, or the storm has ended. */
    public static boolean despawns(int age, boolean storm) {
        return !storm || age >= SpawnerRules.LIFETIME_TICKS;
    }

    /** True on the ticks a devil tosses items. */
    public static boolean tossTick(int age) {
        return age > 0 && age % TOSS_EVERY_TICKS == 0;
    }

    /**
     * Toss velocity for one item: straight up {@link SpawnerRules#TOSS_UP} plus a sideways kick of at most
     * {@link SpawnerRules#TOSS_SIDE} per axis. {@code rx}/{@code rz} are uniform in [0, 1).
     */
    public static double[] tossVelocity(double rx, double rz) {
        double side = SpawnerRules.TOSS_SIDE;
        return new double[] { (clamp01(rx) * 2.0 - 1.0) * side, SpawnerRules.TOSS_UP, (clamp01(rz) * 2.0 - 1.0) * side };
    }

    /** Horizontal wander step for a 2D direction index (0 south, 1 west, 2 north, 3 east — vanilla 2D data values). */
    public static double[] wanderStep(int direction2d) {
        return switch (Math.floorMod(direction2d, 4)) {
            case 0 -> new double[] { 0.0, WANDER_SPEED };
            case 1 -> new double[] { -WANDER_SPEED, 0.0 };
            case 2 -> new double[] { 0.0, -WANDER_SPEED };
            default -> new double[] { WANDER_SPEED, 0.0 };
        };
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) {
            return 0.5;
        }
        return Math.max(0.0, Math.min(0.999_999, v));
    }
}
