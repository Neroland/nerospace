package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.2 Martian Sandstone strata. */
@SuppressWarnings("unused")
class StrataRulesTest {

    @Test
    @DisplayName("exactly four strata variants")
    void four() {
        assertEquals("martian_sandstone_strata_1", StrataRules.variantFor(0));
        assertEquals("martian_sandstone_strata_4", StrataRules.variantFor(3));
        assertThrows(IllegalArgumentException.class, () -> StrataRules.variantFor(4));
        assertThrows(IllegalArgumentException.class, () -> StrataRules.variantFor(-1));
    }
}
