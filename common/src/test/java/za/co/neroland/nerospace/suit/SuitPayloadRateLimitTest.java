package za.co.neroland.nerospace.suit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SuitPayloadRateLimitTest {

    @Test
    void dropsTheExcessWithoutAPlayerId() {
        SuitRateLimit limit = new SuitRateLimit();
        int accepted = 0;
        for (int i = 0; i < 30; i++) {
            if (limit.allow(1, 20)) {
                accepted++;
            }
        }
        assertEquals(20, accepted);
        assertEquals(10, limit.dropped());
        assertTrue(limit.dropped() > 0);
        assertFalse(limit.toString().contains("-"));
    }
}
