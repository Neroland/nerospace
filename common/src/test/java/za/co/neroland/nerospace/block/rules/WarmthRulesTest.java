package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.6 Dawnband Stone. */
@SuppressWarnings("unused")
class WarmthRulesTest {

    @Test
    @DisplayName("shields COLD within 4 only while flaring; light 2 / 11")
    void warmth() {
        assertTrue(WarmthRules.shielded(true, 4.0, 4.0));
        assertFalse(WarmthRules.shielded(true, 4.01, 4.0));
        assertFalse(WarmthRules.shielded(true, 5.0, 10.0), "radius never raised");
        assertFalse(WarmthRules.shielded(false, 0.0, 4.0));
        assertEquals(2, WarmthRules.light(false));
        assertEquals(11, WarmthRules.light(true));
        assertEquals(256, WarmthRules.PER_CHUNK_CAP);
    }

    @Test
    @DisplayName("transition scan: cap per chunk, 64 chunks per tick")
    void scan() {
        assertTrue(WeatherScanRules.underCap(255, 256));
        assertFalse(WeatherScanRules.underCap(256, 256));
        assertEquals(0, WeatherScanRules.ticksFor(0));
        assertEquals(1, WeatherScanRules.ticksFor(64));
        assertEquals(2, WeatherScanRules.ticksFor(65));
    }
}
