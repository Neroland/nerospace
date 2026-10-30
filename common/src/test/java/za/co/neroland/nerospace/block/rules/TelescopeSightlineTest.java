package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Telescope. */
@SuppressWarnings("unused")
class TelescopeSightlineTest {

    @Test
    @DisplayName("1 + min(2, crystals)")
    void reveal() {
        assertEquals(1, TelescopeSightline.revealCount(0));
        assertEquals(2, TelescopeSightline.revealCount(1));
        assertEquals(3, TelescopeSightline.revealCount(2));
        assertEquals(3, TelescopeSightline.revealCount(50));
        assertEquals(1, TelescopeSightline.revealCount(-3));
    }
}
