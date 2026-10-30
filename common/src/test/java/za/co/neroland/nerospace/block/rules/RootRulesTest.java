package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Xertz Soil. */
@SuppressWarnings("unused")
class RootRulesTest {

    @Test
    @DisplayName("only Xertz Soil roots")
    void root() {
        assertTrue(RootRules.canRoot(true));
        assertFalse(RootRules.canRoot(false));
    }
}
