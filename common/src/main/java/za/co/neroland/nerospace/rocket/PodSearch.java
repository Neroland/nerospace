package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;

/**
 * Pure Landing Pod site search (005 §1.7): a Chebyshev spiral out to radius 6 (accepting the first
 * {@code OK} or {@code REUSE} cell in spiral order), then on to radius 24 (first {@code OK}), then a climb of
 * up to 32 blocks starting 8 above the heightmap at the preferred column, taking the first clear box.
 */
public final class PodSearch {

    public static final int INNER_RADIUS = 6;
    public static final int OUTER_RADIUS = 24;
    public static final int MAX_CLIMB = 32;
    /** The climb starts this far above the heightmap at the preferred column. */
    public static final int CLIMB_START = 8;

    /** Samples the world: a column at its heightmap, or an explicit site (the climb). */
    public interface Sampler {
        SiteSample column(int x, int z);

        SiteSample at(BlockPos site);
    }

    /**
     * The search result.
     *
     * @param ringReached the spiral ring the site was found on (or the last ring searched when climbing)
     * @param climbUsed   blocks above the preferred column's heightmap for a climbed pod, else 0
     */
    public record Found(BlockPos site, boolean reused, int ringReached, int climbUsed) {
    }

    private PodSearch() {
    }

    public static Found search(Sampler sampler, BlockPos preferred) {
        return search(sampler, preferred, INNER_RADIUS, OUTER_RADIUS, MAX_CLIMB);
    }

    public static Found search(Sampler sampler, BlockPos preferred, int innerRadius, int outerRadius, int maxClimb) {
        int x = preferred.getX();
        int z = preferred.getZ();
        for (int radius = 0; radius <= outerRadius; radius++) {
            boolean inner = radius <= innerRadius;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    SiteSample sample = sampler.column(x + dx, z + dz);
                    PodSiteRules.SiteVerdict v = PodSiteRules.validate(sample);
                    if (PodSiteRules.accepted(v, inner)) {
                        return new Found(sample.site(), v == PodSiteRules.SiteVerdict.REUSE, radius, 0);
                    }
                }
            }
        }
        // Climb: from heightmap + 8 at the preferred column, the first y whose 5×5×3 box is clear.
        int base = sampler.column(x, z).site().getY();
        BlockPos site = new BlockPos(x, base + CLIMB_START, z);
        int climbed = CLIMB_START;
        for (int step = 0; step < maxClimb; step++) {
            SiteSample s = sampler.at(site);
            if (s.boxClear() && !s.inStructure()) {
                return new Found(site, false, outerRadius, climbed);
            }
            site = site.above();
            climbed++;
        }
        return new Found(site, false, outerRadius, climbed);
    }
}
