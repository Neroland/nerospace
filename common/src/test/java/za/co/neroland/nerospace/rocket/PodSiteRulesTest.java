package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.rocket.PodSiteRules.SiteVerdict;

/** 005 §1.7: each verdict once. */
class PodSiteRulesTest {

    static final BlockPos AT = new BlockPos(0, 64, 0);

    static SiteSample with(boolean[] solid, boolean[] forbidden, boolean[] clear, boolean structure, int claim,
            boolean overlap, boolean pod, boolean reuse) {
        return new SiteSample(AT, solid, forbidden, clear, structure, claim, overlap, pod, reuse);
    }

    static boolean[] t(int n) {
        return SiteSample.fill(n, true);
    }

    static boolean[] f(int n) {
        return SiteSample.fill(n, false);
    }

    @Test
    void ok() {
        assertEquals(SiteVerdict.OK, PodSiteRules.validate(SiteSample.clear(AT)));
    }

    @Test
    void reuse() {
        assertEquals(SiteVerdict.REUSE, PodSiteRules.validate(with(t(25), f(25), t(75), false, 0, false, true, true)));
    }

    @Test
    void floorOneSlab() {
        boolean[] solid = t(25);
        solid[12] = false;
        assertEquals(SiteVerdict.FLOOR, PodSiteRules.validate(with(solid, f(25), t(75), false, 99, false, false, false)));
    }

    @Test
    void fluidOneWaterCell() {
        boolean[] solid = t(25);
        boolean[] forbidden = f(25);
        solid[3] = false;
        forbidden[3] = true;
        assertEquals(SiteVerdict.FLUID_OR_LEAVES,
                PodSiteRules.validate(with(solid, forbidden, t(75), false, 99, false, false, false)));
    }

    @Test
    void structure() {
        assertEquals(SiteVerdict.STRUCTURE, PodSiteRules.validate(with(t(25), f(25), t(75), true, 99, false, false, false)));
    }

    @Test
    void claimFiveRefusedSixFine() {
        assertEquals(SiteVerdict.CLAIM, PodSiteRules.validate(with(t(25), f(25), t(75), false, 5, false, false, false)));
        assertEquals(SiteVerdict.OK, PodSiteRules.validate(with(t(25), f(25), t(75), false, 6, false, false, false)));
    }

    @Test
    void obstructed() {
        boolean[] clear = t(75);
        clear[40] = false; // a block entity in the headroom
        assertEquals(SiteVerdict.OBSTRUCTED, PodSiteRules.validate(with(t(25), f(25), clear, false, 99, false, false, false)));
        assertEquals(SiteVerdict.OBSTRUCTED, PodSiteRules.validate(with(t(25), f(25), t(75), false, 99, true, false, false)));
        // G8: one pod per 32 blocks.
        assertEquals(SiteVerdict.OBSTRUCTED, PodSiteRules.validate(with(t(25), f(25), t(75), false, 99, false, true, false)));
    }
}
