package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §10.2: config may lower a cap, never raise it. */
@SuppressWarnings("unused")
class CapsTest {

    @Test
    @DisplayName("min(locked, configured), floored at 0")
    void lower() {
        assertEquals(48, Caps.lower(48, 100));
        assertEquals(10, Caps.lower(48, 10));
        assertEquals(0, Caps.lower(48, -5));
    }

    @Test
    @DisplayName("frost crystal 64 and dawnband 256 transition caps never raised")
    void transitionCaps() {
        assertEquals(64, Caps.lower(CrystalStageRules.PER_CHUNK_CAP, 1000));
        assertEquals(10, Caps.lower(CrystalStageRules.PER_CHUNK_CAP, 10));
        assertEquals(256, Caps.lower(WarmthRules.PER_CHUNK_CAP, 4096));
        assertEquals(0, Caps.lower(WarmthRules.PER_CHUNK_CAP, 0));
    }
}
