package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.2 Perchlorate oxidiser in the refinery. */
@SuppressWarnings("unused")
class OxidiserRulesTest {

    @Test
    @DisplayName("+15 % with an oxidiser, one per 4 buckets")
    void yield() {
        assertEquals(1.15, OxidiserRules.yieldMultiplier(true));
        assertEquals(1.0, OxidiserRules.yieldMultiplier(false));
        assertEquals(4, OxidiserRules.BUCKETS_PER_OXIDISER);
    }
}
