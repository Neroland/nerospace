package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Spore Pod area cloud. */
@SuppressWarnings("unused")
class SporeBurstRulesTest {

    @Test
    @DisplayName("r 2.5, 100 ticks, Nausea I 8 s; shears/silk take it without bursting")
    void cloud() {
        SporeBurstRules.Cloud c = SporeBurstRules.cloud();
        assertEquals(2.5, c.radius());
        assertEquals(100, c.durationTicks());
        assertEquals(160, c.nauseaTicks());
        assertTrue(c.nauseaTicks() > SporeContactRules.MOSS_NAUSEA_TICKS, "area pod is stronger than point moss");
        assertTrue(SporeBurstRules.bursts(false));
        assertFalse(SporeBurstRules.bursts(true));
        assertEquals(4, SporeBurstRules.BOMB_PODS);
        assertEquals(2, SporeBurstRules.BOMB_OUTPUT);
    }
}
