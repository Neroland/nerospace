package za.co.neroland.nerospace.world.gravity;

import za.co.neroland.nerospace.block.rules.BlockGravityRules;

/**
 * Which non-living entities need a gravity correction, and the column around a player that still entity-ticks.
 * Pure so the scan can shrink without changing the correction itself.
 */
public final class GravityScanRules {

    private GravityScanRules() {
    }

    /**
     * Horizontal reach, in blocks, around a player. A chunk entity-ticks when its Chebyshev chunk distance is within
     * the simulation distance; the far corner of that chunk, with the player in the opposite corner of theirs, is
     * {@code (simulationDistance + 2) * 16} blocks away.
     */
    public static int horizontalReach(int simulationDistanceChunks) {
        int chunks = Math.max(0, simulationDistanceChunks);
        return (chunks + 2) * 16;
    }

    /** Grounded, weightless and submerged entities are left to vanilla, matching the historical correction. */
    public static boolean skipMotion(boolean noGravity, boolean onGround, boolean inWater) {
        return noGravity || onGround || inWater;
    }

    /**
     * Vertical speed after putting back the share of vanilla gravity this tick should not have applied.
     * {@code factor == 1} and {@code gravity == 0} leave {@code dy} unchanged. A negative factor is a Gravity Shard.
     */
    public static double correctedDy(double dy, double gravity, double factor) {
        if (factor == 1.0D || gravity == 0.0D) {
            return dy;
        }
        if (factor < 0.0D) {
            return BlockGravityRules.shardDrift(dy + gravity);
        }
        return dy + gravity * (1.0D - factor);
    }
}
