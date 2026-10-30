package za.co.neroland.nerospace.rocket;

/**
 * Pure Landing Pod site validation. The adapter samples the world into a
 * {@link SiteSample}; this decides.
 */
public final class PodSiteRules {

    /** A site closer than this (Chebyshev) to a claim the rider cannot access is refused. */
    public static final int CLAIM_DISTANCE = 6;
    /** One pod per this many blocks. */
    public static final int POD_SPACING = 32;

    public enum SiteVerdict { OK, REUSE, FLOOR, FLUID_OR_LEAVES, STRUCTURE, CLAIM, OBSTRUCTED }

    private PodSiteRules() {
    }

    /** {@code REUSE} if the site holds an empty pod; else the first failing rule; else {@code OK}. */
    public static SiteVerdict validate(SiteSample s) {
        if (s.existingEmptyPod()) {
            return SiteVerdict.REUSE;
        }
        for (boolean forbidden : s.floorForbidden()) {
            if (forbidden) {
                return SiteVerdict.FLUID_OR_LEAVES;
            }
        }
        for (boolean solid : s.floorSolid()) {
            if (!solid) {
                return SiteVerdict.FLOOR;
            }
        }
        if (s.inStructure()) {
            return SiteVerdict.STRUCTURE;
        }
        if (s.nearestInaccessibleClaimDist() < CLAIM_DISTANCE) {
            return SiteVerdict.CLAIM;
        }
        if (!s.boxClear() || s.overlapsPad() || s.podNearby()) {
            return SiteVerdict.OBSTRUCTED;
        }
        return SiteVerdict.OK;
    }

    /**
     * The climb's rule: {@link #validate} minus the floor rules, which cannot hold in the air —
     * the 5×5×3 box is clear, no structure, at least {@link #CLAIM_DISTANCE} from a claim the rider cannot access,
     * and no overlap with a pad / dock box.
     */
    public static boolean climbAccepts(SiteSample s) {
        return s.boxClear() && !s.inStructure() && s.nearestInaccessibleClaimDist() >= CLAIM_DISTANCE
                && !s.overlapsPad();
    }

    /** Whether a verdict lets the pod land there. */
    public static boolean accepted(SiteVerdict v, boolean allowReuse) {
        return v == SiteVerdict.OK || (allowReuse && v == SiteVerdict.REUSE);
    }
}
