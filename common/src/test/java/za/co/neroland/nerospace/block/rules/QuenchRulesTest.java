package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Ember-Cracked quench. */
@SuppressWarnings("unused")
class QuenchRulesTest {

    @Test
    @DisplayName("any adjacent water quenches")
    void quench() {
        assertFalse(QuenchRules.quenches(new boolean[6]));
        assertTrue(QuenchRules.quenches(new boolean[] {false, false, false, false, false, true}));
        assertEquals(8, QuenchRules.SMOKE_PARTICLES);
    }
}
