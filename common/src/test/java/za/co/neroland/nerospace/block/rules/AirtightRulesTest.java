package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Airtight hull rules. */
@SuppressWarnings("unused")
class AirtightRulesTest {

    @Test
    @DisplayName("tag membership is exactly hull plating + hull window")
    void membership() {
        assertEquals(List.of("nerospace:hull_plating", "nerospace:hull_window"), AirtightRules.AIRTIGHT);
        assertTrue(AirtightRules.isAirtight(true));
        assertFalse(AirtightRules.isAirtight(false));
    }

    @Test
    @DisplayName("station wall seals but is not airtight-counted; console counts sealed modules")
    void counts() {
        assertTrue(AirtightRules.seals(false, true), "legacy station wall still seals");
        assertTrue(AirtightRules.seals(true, false));
        assertFalse(AirtightRules.seals(false, false));
        assertEquals(2, AirtightRules.airtightCount(new boolean[] {true, false, true}));
        assertEquals(1, AirtightRules.countSealed(new boolean[] {true, true, false}, new boolean[] {true, false, true}));
    }
}
