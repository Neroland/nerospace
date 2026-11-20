package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The launch ignition burst is bounded and scales with the tier. */
class IgnitionBurstTest {

    @Test
    void scalesByTierAndIsCapped() {
        assertEquals(36, IgnitionBurst.flames(1));
        assertEquals(48, IgnitionBurst.flames(2));
        assertEquals(60, IgnitionBurst.flames(3));
        assertEquals(72, IgnitionBurst.flames(4));
        assertEquals(IgnitionBurst.MAX_FLAMES, IgnitionBurst.flames(99));
        assertEquals(36, IgnitionBurst.flames(-4));
        for (int tier = 1; tier <= 4; tier++) {
            assertEquals(IgnitionBurst.flames(tier) / 2, IgnitionBurst.smoke(tier));
            assertTrue(IgnitionBurst.spread(tier) > 0.0D && IgnitionBurst.spread(tier) < 0.5D);
        }
    }

    @Test
    void firesOnceOnTheLiftOffTickOnly() throws Exception {
        String rocket = PodArrivalTest.source("RocketEntity.java");
        String tick = PodArrivalTest.body(rocket, "private void tickPresentation()");
        assertTrue(tick.contains("this.launchAnim == 0.0F"), "the burst is gated on the first ascent tick");
        assertEquals(1, tick.split("spawnIgnitionBurst\\(\\)", -1).length - 1);
    }
}
