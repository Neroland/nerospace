package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.4 Packed Cryo-Ice. */
@SuppressWarnings("unused")
class CryoIceRulesTest {

    @Test
    @DisplayName("at least blue-ice friction and never melts")
    void friction() {
        assertTrue(CryoIceRules.friction() >= CryoIceRules.BLUE_ICE);
        assertTrue(CryoIceRules.friction() > 0.98F, "faster than packed ice");
        assertFalse(CryoIceRules.melts(15));
    }
}
