package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

/** 005 §1.7 search: spiral 6 → 24 → climb. */
class PodSearchTest {

    static SiteSample bad(BlockPos at) {
        boolean[] solid = SiteSample.fill(25, true);
        solid[0] = false;
        return new SiteSample(at, solid, SiteSample.fill(25, false), SiteSample.fill(75, true), false,
                Integer.MAX_VALUE, false, false, false);
    }

    static SiteSample reuse(BlockPos at) {
        return new SiteSample(at, SiteSample.fill(25, true), SiteSample.fill(25, false), SiteSample.fill(75, true),
                false, Integer.MAX_VALUE, false, true, true);
    }

    static PodSearch.Sampler sampler(BiFunction<Integer, Integer, SiteSample> column) {
        return new PodSearch.Sampler() {
            @Override
            public SiteSample column(int x, int z) {
                return column.apply(x, z);
            }

            @Override
            public SiteSample at(BlockPos site) {
                return SiteSample.clear(site);
            }
        };
    }

    @Test
    void extendsToRingNine() {
        PodSearch.Found f = PodSearch.search(sampler((x, z) -> {
            BlockPos at = new BlockPos(x, 64, z);
            return Math.max(Math.abs(x), Math.abs(z)) == 9 ? SiteSample.clear(at) : bad(at);
        }), BlockPos.ZERO);
        assertEquals(9, f.ringReached());
        assertEquals(0, f.climbUsed());
    }

    @Test
    void climbsOnlyAfterRing24() {
        PodSearch.Found f = PodSearch.search(sampler((x, z) -> bad(new BlockPos(x, 64, z))), BlockPos.ZERO);
        assertEquals(24, f.ringReached());
        assertTrue(f.climbUsed() > 0);
        assertEquals(64 + PodSearch.CLIMB_START, f.site().getY());
    }

    @Test
    void reuseAcceptedInOrder() {
        // An OK at ring 1 beats a REUSE at ring 2 (spiral order, REUSE is accepted like OK).
        PodSearch.Found f = PodSearch.search(sampler((x, z) -> {
            BlockPos at = new BlockPos(x, 64, z);
            int r = Math.max(Math.abs(x), Math.abs(z));
            return r == 1 ? SiteSample.clear(at) : r == 2 ? reuse(at) : bad(at);
        }), BlockPos.ZERO);
        assertEquals(1, f.ringReached());
        assertFalse(f.reused());
        PodSearch.Found g = PodSearch.search(sampler((x, z) -> {
            BlockPos at = new BlockPos(x, 64, z);
            return Math.max(Math.abs(x), Math.abs(z)) == 2 ? reuse(at) : bad(at);
        }), BlockPos.ZERO);
        assertTrue(g.reused());
        assertEquals(2, g.ringReached());
    }

    @Test
    void reuseNotAcceptedBeyondInnerRing() {
        PodSearch.Found f = PodSearch.search(sampler((x, z) -> {
            BlockPos at = new BlockPos(x, 64, z);
            int r = Math.max(Math.abs(x), Math.abs(z));
            return r == 10 ? reuse(at) : r == 12 ? SiteSample.clear(at) : bad(at);
        }), BlockPos.ZERO);
        assertEquals(12, f.ringReached());
    }

    @Test
    void onePodPer32IsObstructed() {
        SiteSample nearPod = new SiteSample(BlockPos.ZERO, SiteSample.fill(25, true), SiteSample.fill(25, false),
                SiteSample.fill(75, true), false, Integer.MAX_VALUE, false, true, false);
        assertEquals(PodSiteRules.SiteVerdict.OBSTRUCTED, PodSiteRules.validate(nearPod));
    }
}
