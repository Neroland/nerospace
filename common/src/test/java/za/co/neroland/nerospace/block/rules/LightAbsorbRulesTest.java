package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Nyx Stone. */
@SuppressWarnings("unused")
class LightAbsorbRulesTest {

    @Test
    @DisplayName("near Nyx: incoming − 2, floored at 0; elsewhere unchanged")
    void absorb() {
        assertEquals(13, LightAbsorbRules.absorbedLevel(15, true));
        assertEquals(0, LightAbsorbRules.absorbedLevel(1, true));
        assertEquals(0, LightAbsorbRules.absorbedLevel(0, true));
        assertEquals(15, LightAbsorbRules.absorbedLevel(15, false));
    }
}
