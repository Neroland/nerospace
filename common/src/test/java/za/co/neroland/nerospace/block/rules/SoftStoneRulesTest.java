package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Ashstone. */
@SuppressWarnings("unused")
class SoftStoneRulesTest {

    @Test
    @DisplayName("instant with a pickaxe, base otherwise")
    void progress() {
        assertEquals(1.0F, SoftStoneRules.destroyProgress(true, 0.05F));
        assertEquals(0.05F, SoftStoneRules.destroyProgress(false, 0.05F));
    }
}
