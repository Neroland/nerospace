package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;

/**
 * Pure Landing Pod site search: a Chebyshev spiral out to radius 6 (accepting the first
 * {@code OK} or {@code REUSE} cell in spiral order), then on to radius 24 (first {@code OK}), then a climb of
 * up to 32 blocks starting 8 above the heightmap at the preferred column, taking the first site whose box is clear,
 * outside every structure, at least {@link PodSiteRules#CLAIM_DISTANCE} from an inaccessible claim and clear of
 * every pad box ({@link PodSiteRules#climbAccepts}). When the climb runs out it falls back to the highest box-clear
 * site it saw; it never returns a site whose box is obstructed while a clear one was found.
 */
public final class PodSearch {

    public static final int INNER_RADIUS = 6;
    public static final int OUTER_RADIUS = 24;
    public static final int MAX_CLIMB = 32;
    /** The climb starts this far above the heightmap at the preferred column. */
    public static final int CLIMB_START = 8;

    /** Samples the world: a column at its heightmap, or an explicit site (the climb). */
    public interface Sampler {
        /** Called once before the search with the column it starts from (the adapter fixes its chunk budget). */
        default void begin(BlockPos preferred) {
        }

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
        sampler.begin(preferred);
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
        // Climb: from heightmap + 8 at the preferred column, the first y the climb rules accept
        // (box clear, no structure, claim distance and pad overlap — the floor rules do not apply in the air).
        int base = sampler.column(x, z).site().getY();
        BlockPos site = new BlockPos(x, base + CLIMB_START, z);
        int climbed = CLIMB_START;
        BlockPos highestClear = null;
        int highestClimb = 0;
        BlockPos last = site;
        int lastClimb = climbed;
        for (int step = 0; step < maxClimb; step++) {
            SiteSample s = sampler.at(site);
            if (PodSiteRules.climbAccepts(s)) {
                return new Found(site, false, outerRadius, climbed);
            }
            if (s.boxClear() && !s.inStructure()) {
                highestClear = site;
                highestClimb = climbed;
            }
            last = site;
            lastClimb = climbed;
            site = site.above();
            climbed++;
        }
        if (highestClear != null) {
            return new Found(highestClear, false, outerRadius, highestClimb);
        }
        return new Found(last, false, outerRadius, lastClimb); // nothing clear anywhere: the top of the climb
    }
}
