package za.co.neroland.nerospace.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Snapshots are skipped only when the client already has this view. */
class OxygenSyncRulesTest {

    @Test
    @DisplayName("a still player on an unchanged field skips the walk; a move or a new revision does not")
    void skipBuild() {
        assertFalse(OxygenSyncRules.skipBuild(false, 0, 1L, 0, 1L));
        assertTrue(OxygenSyncRules.skipBuild(true, 3, 9L, 3, 9L));
        assertFalse(OxygenSyncRules.skipBuild(true, 3, 9L, 4, 9L));
        assertFalse(OxygenSyncRules.skipBuild(true, 3, 9L, 3, 10L));
    }

    @Test
    @DisplayName("an equal fingerprint is not sent again; the first view always is")
    void skipSend() {
        assertFalse(OxygenSyncRules.skipSend(false, 5L, 5L));
        assertTrue(OxygenSyncRules.skipSend(true, 5L, 5L));
        assertFalse(OxygenSyncRules.skipSend(true, 5L, 6L));
    }

    @Test
    @DisplayName("the fingerprint depends on the cells, not on insertion order")
    void fingerprintIsOrderIndependent() {
        Long2ByteOpenHashMap left = map();
        left.put(10L, (byte) 1);
        left.put(20L, (byte) 15);
        Long2ByteOpenHashMap right = map();
        right.put(20L, (byte) 15);
        right.put(10L, (byte) 1);
        assertEquals(OxygenSyncRules.fingerprint(left), OxygenSyncRules.fingerprint(right));
        right.put(20L, (byte) 14);
        assertFalse(OxygenSyncRules.fingerprint(left) == OxygenSyncRules.fingerprint(right));
    }

    private static Long2ByteOpenHashMap map() {
        Long2ByteOpenHashMap m = new Long2ByteOpenHashMap();
        m.defaultReturnValue((byte) 0);
        return m;
    }
}
