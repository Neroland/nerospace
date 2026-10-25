package za.co.neroland.nerospace.world.planet.nyxara;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.nyxara.worldgen.BandWidthRules;
import za.co.neroland.nerospace.world.planet.nyxara.worldgen.FixedXBands;
import za.co.neroland.nerospace.world.planet.nyxara.worldgen.FixedXBands.Side;

/** X → side mapping of the {@code nerospace:fixed_x} biome source (003 §7.3) and the stored-width rule. */
class FixedXBandsTest {

    private static final int[] WIDTHS = {FixedXBands.MIN_WIDTH, FixedXBands.DEFAULT_WIDTH, FixedXBands.MAX_WIDTH};
    private static final int[] ZS = {0, 17, -999, 123_457, -30_000_000, 30_000_000};

    @Test
    void exactEdgesWithoutNoise() {
        for (int w : WIDTHS) {
            int half = w / 2;
            for (int z : ZS) {
                assertEquals(Side.BAND, FixedXBands.sideAt(0, z, w, 0, 0));
                assertEquals(Side.BAND, FixedXBands.sideAt(-half, z, w, 0, 0), "x = -w/2 is band, w=" + w);
                assertEquals(Side.BAND, FixedXBands.sideAt(half, z, w, 0, 0), "x = +w/2 is band, w=" + w);
                assertEquals(Side.WEST, FixedXBands.sideAt(-half - 1, z, w, 0, 0), "dayside west, w=" + w);
                assertEquals(Side.EAST, FixedXBands.sideAt(half + 1, z, w, 0, 0), "nightside east, w=" + w);
            }
        }
    }

    @Test
    void extremeCoordinates() {
        for (int w : WIDTHS) {
            for (int z : ZS) {
                assertEquals(Side.WEST, FixedXBands.sideAt(-30_000_000, z, w, 0, FixedXBands.DEFAULT_EDGE_NOISE));
                assertEquals(Side.EAST, FixedXBands.sideAt(30_000_000, z, w, 0, FixedXBands.DEFAULT_EDGE_NOISE));
                assertEquals(Side.WEST, FixedXBands.sideAt(Integer.MIN_VALUE, z, w, 0, FixedXBands.DEFAULT_EDGE_NOISE));
                assertEquals(Side.EAST, FixedXBands.sideAt(Integer.MAX_VALUE, z, w, 0, FixedXBands.DEFAULT_EDGE_NOISE));
            }
        }
        // a far origin does not overflow either
        assertEquals(Side.BAND, FixedXBands.sideAt(30_000_000, 5, 2048, 30_000_000, 96));
        assertEquals(Side.WEST, FixedXBands.sideAt(-30_000_000, 5, 2048, 30_000_000, 96));
    }

    @Test
    void edgeNoiseIsBoundedAndRagged() {
        int w = FixedXBands.DEFAULT_WIDTH;
        int n = FixedXBands.DEFAULT_EDGE_NOISE;
        int half = w / 2;
        boolean westMoved = false;
        for (int z = -4096; z <= 4096; z += 7) {
            // always band well inside, always the side well outside
            assertEquals(Side.BAND, FixedXBands.sideAt(-half + n + 1, z, w, 0, n));
            assertEquals(Side.BAND, FixedXBands.sideAt(half - n - 1, z, w, 0, n));
            assertEquals(Side.WEST, FixedXBands.sideAt(-half - n - 1, z, w, 0, n));
            assertEquals(Side.EAST, FixedXBands.sideAt(half + n + 1, z, w, 0, n));
            if (FixedXBands.sideAt(-half - n / 2, z, w, 0, n) != FixedXBands.sideAt(-half - n / 2, z + 512, w, 0, n)) {
                westMoved = true;
            }
        }
        assertTrue(westMoved, "the terminator should be ragged along Z, not a straight wall");
    }

    @Test
    void deterministic() {
        for (int x = -1200; x <= 1200; x += 37) {
            for (int z = -300; z <= 300; z += 53) {
                assertEquals(FixedXBands.sideAt(x, z, 2048, 0, 96), FixedXBands.sideAt(x, z, 2048, 0, 96));
            }
        }
    }

    @Test
    void originShiftsTheBand() {
        assertEquals(Side.BAND, FixedXBands.sideAt(5000, 0, 2048, 5000, 0));
        assertEquals(Side.WEST, FixedXBands.sideAt(0, 0, 2048, 5000, 0));
        assertEquals(Side.EAST, FixedXBands.sideAt(10_000, 0, 2048, 5000, 0));
    }

    @Test
    void widthClampedToConfigRange() {
        assertEquals(512, FixedXBands.clampWidth(1));
        assertEquals(16384, FixedXBands.clampWidth(1_000_000));
        // a too-small width behaves as 512
        assertEquals(Side.BAND, FixedXBands.sideAt(256, 0, 10, 0, 0));
        assertEquals(Side.EAST, FixedXBands.sideAt(257, 0, 10, 0, 0));
    }

    @Test
    void storedWidthWinsOverConfig() {
        assertEquals(2048, BandWidthRules.resolve(-1, 2048), "first load takes the config");
        assertEquals(4096, BandWidthRules.resolve(4096, 1024), "a stored width is never reshaped by config");
        assertEquals(16384, BandWidthRules.resolve(99_999, 2048), "a hand-edited value is clamped");
        assertEquals(512, BandWidthRules.resolve(0, 64), "config below range is clamped");
    }

    @Test
    void probeOriginsHitAllThreeBands() {
        int w = FixedXBands.DEFAULT_WIDTH;
        int n = FixedXBands.DEFAULT_EDGE_NOISE;
        assertEquals(Side.WEST, FixedXBands.sideAt(-4096, 0, w, 0, n));
        assertEquals(Side.BAND, FixedXBands.sideAt(0, 0, w, 0, n));
        assertEquals(Side.EAST, FixedXBands.sideAt(4096, 0, w, 0, n));
        assertEquals(Side.EAST, FixedXBands.sideAt(1536, -2048, w, 0, n));
    }
}
