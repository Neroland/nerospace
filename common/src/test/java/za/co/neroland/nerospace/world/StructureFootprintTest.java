package za.co.neroland.nerospace.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.mars.worldgen.DerelictHabitatFeature;
import za.co.neroland.nerospace.world.planet.mars.worldgen.RoverWreckFeature;

/**
 * Every alien/Mars structure writes only inside its feature's 3×3-chunk region (the 1.6.0 mega city did not: the
 * Stage 3C probe logged 888 "setBlock in a far chunk" lines), and {@link StructureSpacing} with salt 0 is the 1.6.0
 * hash so existing worlds' structure positions never move.
 */
class StructureFootprintTest {

    @Test
    void megaCityFootprintStaysInsideItsRegion() {
        int r = MegaCityLayout.footprintRadius();
        assertEquals(MegaCityLayout.WALL_R, r, "the wall is the outermost ring");
        for (int origin = -2048; origin <= 2048; origin++) {
            StructureRegion region = StructureRegion.around(origin, origin);
            int c = MegaCityLayout.centreFor(origin);
            assertEquals(origin >> 4, c >> 4, "centre left the origin chunk at " + origin);
            assertTrue(region.containsSquare(c, c, r), "mega city spills out of its region at origin " + origin);
        }
    }

    @Test
    void the160MegaCityPlacementWasTheBug() {
        // Centred on the in_square origin, the 41×41 city leaves the region whenever the origin is near a chunk edge.
        StructureRegion region = StructureRegion.around(-1226, 444);
        assertFalse(region.containsSquare(-1226, 444, MegaCityLayout.WALL_R));
    }

    @Test
    void smallStructuresFitFromAnyOrigin() {
        // An origin anywhere in its chunk is ≤ 15 blocks from either edge, so a half-size ≤ 16 always fits.
        for (int half : new int[] {RoverWreckFeature.FOOTPRINT_R, DerelictHabitatFeature.FOOTPRINT_R, 6 /* hamlet */,
                7 /* ruin podium */}) {
            assertTrue(half <= 16, "footprint " + half);
            for (int origin = -64; origin < 64; origin++) {
                assertTrue(StructureRegion.around(origin, origin).containsSquare(origin, origin, half));
            }
        }
    }

    @Test
    void saltZeroIsThe160Hash() {
        for (int x = -40; x <= 40; x++) {
            for (int z = -40; z <= 40; z++) {
                assertEquals(legacyMix(x, z), StructureSpacing.mix(x, z, 0L));
            }
        }
    }

    @Test
    void villageBandsAreUnchangedAndMarsKindsNeverShareACell() {
        int hamlets = 0;
        int differs = 0;
        for (int cx = -400; cx < 400; cx += 16) {
            for (int cz = -400; cz < 400; cz += 16) {
                long h = StructureSpacing.mix(Math.floorDiv(cx, 16), Math.floorDiv(cz, 16), 0L);
                int roll = (int) Math.floorMod(h, 100L);
                StructureSpacing.Roi legacy = roll < 26 ? StructureSpacing.Roi.HAMLET
                        : roll < 34 ? StructureSpacing.Roi.RUIN
                        : roll < 36 ? StructureSpacing.Roi.MEGA_CITY : null;
                StructureSpacing.Roi now = StructureSpacing.roiFor(h);
                if (legacy != null) {
                    assertEquals(legacy, now);
                    hamlets += legacy == StructureSpacing.Roi.HAMLET ? 1 : 0;
                } else {
                    assertTrue(now == StructureSpacing.Roi.NONE || now == StructureSpacing.Roi.ROVER_WRECK
                            || now == StructureSpacing.Roi.DERELICT_HABITAT);
                }
                long salted = StructureSpacing.mix(Math.floorDiv(cx, 16), Math.floorDiv(cz, 16), 0x4D415253L);
                differs += salted != h ? 1 : 0;
            }
        }
        assertTrue(hamlets > 0);
        assertTrue(differs > 0, "the Mars salt must move cells");
    }

    /** The 1.6.0 {@code StructureSpacing.mix}, verbatim. */
    private static long legacyMix(int x, int z) {
        long h = x * 341873128712L + z * 132897987541L + 0x9E3779B97F4A7C15L;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return h;
    }
}
