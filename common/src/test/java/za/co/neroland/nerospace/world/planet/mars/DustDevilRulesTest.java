package za.co.neroland.nerospace.world.planet.mars;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.block.rules.SpawnerRules;
import za.co.neroland.nerospace.world.planet.mars.entity.DustDevilRules;

class DustDevilRulesTest {

    @Test
    void spawnNeedsStormRollAndRoom() {
        assertTrue(DustDevilRules.maySpawn(true, 0, 0, true));
        assertTrue(DustDevilRules.maySpawn(true, 0, 1, true));
        assertFalse(DustDevilRules.maySpawn(false, 0, 0, true), "never outside a storm");
        assertFalse(DustDevilRules.maySpawn(true, 0, 0, false), "lost roll");
        assertFalse(DustDevilRules.maySpawn(true, 1, 0, true), "a devil within 32 blocks blocks the pit");
        assertFalse(DustDevilRules.maySpawn(true, 0, 2, true), "at most two in the 3x3 chunk area");
    }

    @Test
    void despawnsAtLifetimeOrStormEnd() {
        assertFalse(DustDevilRules.despawns(0, true));
        assertFalse(DustDevilRules.despawns(SpawnerRules.LIFETIME_TICKS - 1, true));
        assertTrue(DustDevilRules.despawns(SpawnerRules.LIFETIME_TICKS, true));
        assertTrue(DustDevilRules.despawns(5, false), "storm over -> gone at once");
    }

    @Test
    void tossIsUpwardWithBoundedSideKick() {
        for (double r : new double[] { 0.0, 0.25, 0.5, 0.75, 0.999, 1.0, -3.0, 7.0, Double.NaN }) {
            double[] v = DustDevilRules.tossVelocity(r, r);
            assertEquals(SpawnerRules.TOSS_UP, v[1], 1e-9);
            assertTrue(Math.abs(v[0]) <= SpawnerRules.TOSS_SIDE + 1e-9, "x " + v[0]);
            assertTrue(Math.abs(v[2]) <= SpawnerRules.TOSS_SIDE + 1e-9, "z " + v[2]);
        }
        assertArrayEquals(new double[] { 0.0, SpawnerRules.TOSS_UP, 0.0 }, DustDevilRules.tossVelocity(0.5, 0.5), 1e-9);
    }

    @Test
    void tossCadenceAndWander() {
        assertFalse(DustDevilRules.tossTick(0));
        assertTrue(DustDevilRules.tossTick(DustDevilRules.TOSS_EVERY_TICKS));
        assertFalse(DustDevilRules.tossTick(DustDevilRules.TOSS_EVERY_TICKS + 1));
        for (int d = -4; d < 8; d++) {
            double[] s = DustDevilRules.wanderStep(d);
            assertEquals(DustDevilRules.WANDER_SPEED, Math.abs(s[0]) + Math.abs(s[1]), 1e-9);
        }
        assertArrayEquals(new double[] { 0.0, DustDevilRules.WANDER_SPEED }, DustDevilRules.wanderStep(0), 1e-9);
    }
}
