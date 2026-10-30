package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A landing never stacks rockets, even when the parked rocket's chunk entities load after the resolver ran: the
 * descent re-checks the target through the resolver adapter and touchdown falls back to a Landing Pod.
 */
class LandingOccupancyTest {

    @Test
    void onlyAnotherParkedRocketBlocksTheTarget() {
        assertTrue(LandingResolverLevelAdapter.blocksLanding(false, false, false));  // parked rocket
        assertFalse(LandingResolverLevelAdapter.blocksLanding(true, false, false));  // the descending rocket itself
        assertFalse(LandingResolverLevelAdapter.blocksLanding(false, true, false));  // lifting off
        assertFalse(LandingResolverLevelAdapter.blocksLanding(false, false, true));  // still coming down itself
    }

    @Test
    void anOccupiedOrBrokenTargetIsLeft() {
        assertTrue(LandingResolverLevelAdapter.mustLeaveTarget(false, false));  // pad broken
        assertTrue(LandingResolverLevelAdapter.mustLeaveTarget(true, true));    // rocket appeared on it
        assertFalse(LandingResolverLevelAdapter.mustLeaveTarget(true, false));  // free
    }

    @Test
    void descentRechecksOccupancyAndTouchdownFallsBackToAPod() throws Exception {
        String entity = PodArrivalTest.source("RocketEntity.java");
        String descent = PodArrivalTest.body(entity, "private void tickDescent(ServerLevel level)");
        assertTrue(descent.contains("LandingResolverLevelAdapter.targetOccupied("),
                "every pad check must look for a rocket that loaded onto the target");
        assertTrue(descent.contains("LandingResolverLevelAdapter.entitiesReady("),
                "touchdown must wait until the target chunk's entities are loaded");
        String touchdown = PodArrivalTest.body(entity, "private void touchdown(ServerLevel level, boolean aborted)");
        int check = touchdown.indexOf("LandingResolverLevelAdapter.targetOccupied(");
        int fallback = touchdown.indexOf("LandingResolverLevelAdapter.podFallback(");
        int settle = touchdown.indexOf("setPhase(DescentMath.Phase.TOUCHDOWN)");
        assertTrue(check >= 0 && fallback > check && settle > fallback,
                "touchdown must check the target and fall back to a pod before it settles");
    }
}
