package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Magma Glass. */
@SuppressWarnings("unused")
class LavaImmuneRulesTest {

    @Test
    @DisplayName("lava never replaces it")
    void immune() {
        assertFalse(LavaImmuneRules.replaceable(true));
        assertFalse(LavaImmuneRules.replaceable(false));
    }
}
