package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;

import za.co.neroland.nerospace.config.NerospaceConfig;

/**
 * Calculates the fuel a launch burns, based on the trip. A same-dimension hop (pad-to-pad across the same
 * world) costs a base amount plus a per-block distance rate; crossing into another dimension costs the
 * base plus a flat surcharge (a cross-dimension distance has no meaningful block measure). The result is
 * scaled by the {@code fuelCost} config multiplier, like {@link RocketTier#fuelPerLaunch()}.
 */
public final class RocketTravel {

    /** Flat base cost of any launch, in millibuckets. */
    public static final int BASE = 500;
    /** Millibuckets burned per 100 blocks of same-dimension distance. */
    public static final int RATE_PER_100 = 60;
    /** Flat surcharge for crossing into another dimension, in millibuckets. */
    public static final int CROSS_DIM = 2000;

    private RocketTravel() {
    }

    /** Horizontal block distance between two positions (Y ignored — travel is a map hop). */
    public static int distance(BlockPos from, BlockPos to) {
        double dx = from.getX() - to.getX();
        double dz = from.getZ() - to.getZ();
        return (int) Math.sqrt(dx * dx + dz * dz);
    }

    /** Raw (unscaled) fuel cost for a trip. */
    public static int rawCost(boolean crossDimension, int distance) {
        long cost = BASE;
        if (crossDimension) {
            cost += CROSS_DIM;
        } else {
            cost += (long) Math.max(0, distance) * RATE_PER_100 / 100L;
        }
        return (int) Math.min(Integer.MAX_VALUE - 1, Math.max(1, cost));
    }

    /** Config-scaled fuel cost for a trip, in millibuckets. */
    public static int cost(boolean crossDimension, int distance) {
        return NerospaceConfig.scale(rawCost(crossDimension, distance), NerospaceConfig.fuelCostMultiplier());
    }
}
