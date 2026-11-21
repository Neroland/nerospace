package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.europa.entity.EuropaFaunaRules;

/** The pure behaviour numbers of the Lantern Drifter, Vent Ray and Shell Skater (design 9.1-9.3). */
class EuropaFaunaRulesTest {

    @Test
    @DisplayName("drifter: a pulse every 30-50 ticks, up toward light >= 7, level when lit, down otherwise")
    void drifter() {
        assertEquals(30, EuropaFaunaRules.pulseInterval(0));
        assertEquals(50, EuropaFaunaRules.pulseInterval(20));
        assertEquals(50, EuropaFaunaRules.pulseInterval(99));
        assertEquals(30, EuropaFaunaRules.pulseInterval(-5));
        assertTrue(EuropaFaunaRules.drifterLift(7, 0) > 0);
        assertTrue(EuropaFaunaRules.drifterLift(15, 15) > 0);
        assertEquals(0.0D, EuropaFaunaRules.drifterLift(6, 7));
        assertTrue(EuropaFaunaRules.drifterLift(6, 6) < 0);
        assertEquals(200, EuropaFaunaRules.strandedAir(300));
        assertEquals(50, EuropaFaunaRules.strandedAir(50));
    }

    @Test
    @DisplayName("drifter schooling: the pulse leans toward the mate and stays a unit vector")
    void school() {
        double[] alone = EuropaFaunaRules.pulseDirection(0.0D, false, 0, 0);
        assertEquals(1.0D, alone[0], 1e-9);
        assertEquals(0.0D, alone[1], 1e-9);
        double[] lean = EuropaFaunaRules.pulseDirection(Math.PI, true, 5.0D, 0.0D);
        assertTrue(lean[0] > 0, "random points away, the mate wins with a pull above one half");
        assertEquals(1.0D, Math.hypot(lean[0], lean[1]), 1e-9);
        double[] onTop = EuropaFaunaRules.pulseDirection(1.0D, true, 0.0D, 0.0D);
        assertEquals(Math.cos(1.0D), onTop[0], 1e-9);
    }

    @Test
    @DisplayName("ray: the vent search is bounded (<= 400 reads, inside 25 x 9 x 25), nearest first, no repeats")
    void raySearch() {
        List<int[]> scan = EuropaFaunaRules.scanOffsets();
        assertEquals(EuropaFaunaRules.SCAN_MAX_READS, scan.size());
        assertTrue(scan.size() <= 400);
        Set<String> seen = new HashSet<>();
        int last = -1;
        for (int[] o : scan) {
            assertTrue(Math.abs(o[0]) <= 12 && Math.abs(o[1]) <= 4 && Math.abs(o[2]) <= 12);
            int d = o[0] * o[0] + o[1] * o[1] + o[2] * o[2];
            assertTrue(d >= last, "nearest first");
            last = d;
            assertTrue(seen.add(o[0] + "," + o[1] + "," + o[2]));
        }
        assertEquals(0, scan.get(0)[0] * scan.get(0)[0] + scan.get(0)[1] * scan.get(0)[1] + scan.get(0)[2] * scan.get(0)[2]);
        assertEquals(200, EuropaFaunaRules.RETARGET_TICKS);
    }

    /** V4-03: over ice a skater may spawn in the dark (value >= 0); blades stay avoided; elsewhere unchanged. */
    @Test
    @DisplayName("skater: walk value never negative over ice, blades -10, other ground inherited")
    void skaterWalkValue() {
        assertEquals(0.0F, EuropaFaunaRules.skaterWalkValue(false, true, -0.5F));
        assertEquals(10.0F, EuropaFaunaRules.skaterWalkValue(false, true, 10.0F));
        assertEquals(-0.5F, EuropaFaunaRules.skaterWalkValue(false, false, -0.5F));
        assertEquals(EuropaFaunaRules.BLADE_AVOIDANCE, EuropaFaunaRules.skaterWalkValue(true, true, 10.0F));
    }

    /** V4-02: the search reaches the circle and beyond (7-12 blocks), and a cached vent survives a retarget. */
    @Test
    @DisplayName("ray: the lattice search reaches a vent 7-12 blocks away; the cached vent is kept within the leash")
    void raySearchReach() {
        // One pass already reaches 12 blocks out along an axis and the full circle radius in every direction.
        int farthest = 0;
        for (int[] o : EuropaFaunaRules.scanOffsets(0)) {
            farthest = Math.max(farthest, o[0] * o[0] + o[2] * o[2]);
        }
        assertTrue(farthest >= 144, "one search reaches 12 blocks horizontally");
        // Every cell within 12 blocks horizontally (|dy| <= 4) is read by exactly one of the passes.
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                if (dx * dx + dz * dz > 144) {
                    continue;
                }
                for (int dy = -4; dy <= 4; dy++) {
                    int hits = 0;
                    for (int pass = 0; pass < EuropaFaunaRules.SCAN_PASSES; pass++) {
                        for (int[] o : EuropaFaunaRules.scanOffsets(pass)) {
                            if (o[0] == dx && o[1] == dy && o[2] == dz) {
                                hits++;
                            }
                        }
                    }
                    assertEquals(1, hits, dx + "," + dy + "," + dz);
                }
            }
        }
        for (int pass = 0; pass < EuropaFaunaRules.SCAN_PASSES; pass++) {
            assertTrue(EuropaFaunaRules.scanOffsets(pass).size() <= EuropaFaunaRules.SCAN_MAX_READS);
        }
        // A ray on its circle (7.4 blocks from the vent) keeps it; past the leash, unloaded or inactive it does not.
        assertTrue(EuropaFaunaRules.keepVent(true, true, 7.4D * 7.4D));
        assertTrue(EuropaFaunaRules.keepVent(true, true, 144.0D));
        assertFalse(EuropaFaunaRules.keepVent(true, true, 144.1D));
        assertFalse(EuropaFaunaRules.keepVent(false, true, 1.0D));
        assertFalse(EuropaFaunaRules.keepVent(true, false, 1.0D));
        assertTrue(EuropaFaunaRules.SETTLE_REACH >= EuropaFaunaRules.CIRCLE_RADIUS + 1.0D, "settles on its circle");
    }

    @Test
    @DisplayName("ray: circles inside 12 of its vent, settles 100-300 ticks, flees a player within 3 for 6 blocks")
    void rayBehaviour() {
        double[] p = EuropaFaunaRules.circlePoint(10.0D, -4.0D, 1.3D);
        assertEquals(EuropaFaunaRules.CIRCLE_RADIUS, Math.hypot(p[0] - 10.0D, p[1] + 4.0D), 1e-9);
        assertTrue(EuropaFaunaRules.CIRCLE_RADIUS <= EuropaFaunaRules.LEASH && EuropaFaunaRules.LEASH == 12.0D);
        assertEquals(100, EuropaFaunaRules.settleTicks(0));
        assertEquals(300, EuropaFaunaRules.settleTicks(1000));
        assertTrue(EuropaFaunaRules.rayFlees(8.99D));
        assertFalse(EuropaFaunaRules.rayFlees(9.0D));
        assertEquals(6.0D, EuropaFaunaRules.FLEE_DISTANCE);
        assertEquals(0.1D, EuropaFaunaRules.steer(0.0D, 1.0D), 1e-12);
        assertTrue(EuropaFaunaRules.RAY_MAX_SPAWN_Y == 0, "spawns only below Y 0");
    }

    @Test
    @DisplayName("skater: keeps most of its speed on ice, never pushed past the glide cap, licks on a cooldown")
    void skater() {
        assertEquals(0.92D * 0.2D, EuropaFaunaRules.glide(0.2D, 0.05D), 1e-12);
        assertEquals(0.25D, EuropaFaunaRules.glide(0.1D, 0.25D), 1e-12);
        assertEquals(EuropaFaunaRules.MAX_GLIDE, EuropaFaunaRules.glide(1.0D, 0.1D), 1e-12);
        assertEquals(0.5D, EuropaFaunaRules.glide(1.0D, 0.5D), 1e-12, "own speed is never reduced");
        assertEquals(200, EuropaFaunaRules.lickCooldown(0));
        assertEquals(400, EuropaFaunaRules.lickCooldown(1000));
        assertTrue(EuropaFaunaRules.BLADE_AVOIDANCE < 0);
    }
}
