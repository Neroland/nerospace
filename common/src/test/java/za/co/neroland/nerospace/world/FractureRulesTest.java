package za.co.neroland.nerospace.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.europa.EuropaNumbers;

/** The ice-fracture plan: size, reach, fill and the depth it is cut to. */
class FractureRulesTest {

    @Test
    @DisplayName("lengths run 9 to 17; a plan is length x 2 distinct columns within 9 of the origin")
    void plan() {
        assertEquals(9, FractureRules.length(0));
        assertEquals(17, FractureRules.length(8));
        assertEquals(17, FractureRules.length(99));
        for (boolean alongX : new boolean[] {true, false}) {
            for (int length = FractureRules.LENGTH_MIN; length <= FractureRules.LENGTH_MAX; length++) {
                List<FractureRules.Column> plan = FractureRules.plan(1000, -2000, alongX, length);
                assertEquals(length * FractureRules.WIDTH, plan.size());
                assertEquals(plan.size(), new HashSet<>(plan).size(), "no column twice");
                for (FractureRules.Column c : plan) {
                    assertTrue(Math.abs(c.x() - 1000) <= FractureRules.MAX_REACH, c.toString());
                    assertTrue(Math.abs(c.z() + 2000) <= FractureRules.MAX_REACH, c.toString());
                }
                assertTrue(plan.contains(new FractureRules.Column(1000, -2000)), "centred on the origin");
            }
        }
        assertEquals(FractureRules.LENGTH_MAX * FractureRules.WIDTH, FractureRules.plan(0, 0, true, 40).size());
        assertEquals(9, FractureRules.MAX_REACH);
    }

    @Test
    @DisplayName("water below sea level, air at and above it; cut down to the lowest shell underside")
    void fill() {
        assertEquals("minecraft:water", FractureRules.fill(87));
        assertEquals("minecraft:air", FractureRules.fill(88));
        assertEquals("minecraft:water", FractureRules.fill(FractureRules.BOTTOM_Y));
        assertEquals(EuropaNumbers.ROOF_MIN, FractureRules.BOTTOM_Y);
        assertEquals(EuropaNumbers.SEA_LEVEL, FractureRules.SEA_LEVEL);
        assertFalse(FractureRules.BOTTOM_Y >= FractureRules.SEA_LEVEL);
    }
}
