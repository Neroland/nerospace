package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.2 Basalt Column. */
@SuppressWarnings("unused")
class PillarRulesTest {

    @Test
    @DisplayName("grows to 4 into air: ≤ 3 extensions per placement")
    void grow() {
        assertEquals(1, PillarRules.nextSegment(1, 4, true));
        assertEquals(2, PillarRules.nextSegment(2, 4, true));
        assertEquals(3, PillarRules.nextSegment(3, 4, true));
        assertEquals(-1, PillarRules.nextSegment(4, 4, true));
        assertEquals(-1, PillarRules.nextSegment(2, 4, false), "stops at obstruction");
        assertEquals(-1, PillarRules.nextSegment(4, 10, true), "max never above 4");
        assertEquals(-1, PillarRules.nextSegment(0, 4, true));
        int extensions = 0;
        for (int h = 1; PillarRules.nextSegment(h, 4, true) >= 0; h++) {
            extensions++;
        }
        assertEquals(3, extensions);
        assertEquals(20, PillarRules.DELAY_TICKS);
    }
}
