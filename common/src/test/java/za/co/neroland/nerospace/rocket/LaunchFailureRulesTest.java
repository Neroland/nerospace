package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A missing destination level must not spend the rocket. The entity method order is checked against the source. */
class LaunchFailureRulesTest {

    @Test
    @DisplayName("a loaded destination travels; a missing one keeps the hull")
    void abortOnlyWhenTheLevelIsMissing() {
        assertFalse(LaunchFailureRules.abort(true));
        assertTrue(LaunchFailureRules.abort(false));
    }

    @Test
    @DisplayName("the abort stand is the pad block plus the 3px plate, centred")
    void padStandMatchesTheLaunchPlate() {
        assertEquals(10.5D, LaunchFailureRules.padStand(10, 64, -3).x);
        assertEquals(64.0D + LandingResolver.SURFACE_HEIGHT, LaunchFailureRules.padStand(10, 64, -3).y);
        assertEquals(-2.5D, LaunchFailureRules.padStand(10, 64, -3).z);
    }

    @Test
    @DisplayName("completeLaunch0 aborts before it drains fuel or discards the hull")
    void entityAbortsBeforeSpendingTheRocket() throws Exception {
        String body = PodArrivalTest.body(PodArrivalTest.source("RocketEntity.java"), "private void completeLaunch0()");
        int abort = body.indexOf("abortMissingDestination(");
        int drain = body.indexOf("this.fuelTank.drain(");
        int discard = body.indexOf("this.discard()");
        assertTrue(abort > 0 && drain > abort && discard > drain, body);
        assertTrue(body.contains(LaunchFailureRules.DESTINATION_MISSING) || PodArrivalTest
                .source("RocketEntity.java").contains(LaunchFailureRules.DESTINATION_MISSING));
    }
}
