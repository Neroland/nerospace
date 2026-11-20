package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Chaos Ice rafts. */
class RaftFallRulesTest {

    @Test
    @DisplayName("an unsupported group of up to 6 falls, lowest first")
    void falls() {
        long[] plan = RaftFallRules.plan(new long[] {30L, 10L, 20L}, new int[] {70, 71, 70},
                new boolean[] {false, false, false}, false);
        assertArrayEquals(new long[] {20L, 30L, 10L}, plan);
        long[] six = RaftFallRules.plan(new long[] {1, 2, 3, 4, 5, 6}, new int[6], new boolean[6], false);
        assertEquals(6, six.length);
    }

    @Test
    @DisplayName("one supported member holds the whole group")
    void supported() {
        assertEquals(0, RaftFallRules.plan(new long[] {1, 2, 3}, new int[3], new boolean[] {false, true, false},
                false).length);
    }

    @Test
    @DisplayName("never more than the cap: a larger or truncated group stays")
    void cap() {
        assertEquals(0, RaftFallRules.plan(new long[] {1, 2, 3, 4, 5, 6, 7}, new int[7], new boolean[7], false).length);
        assertEquals(0, RaftFallRules.plan(new long[] {1, 2, 3, 4, 5, 6}, new int[6], new boolean[6], true).length);
        assertEquals(0, RaftFallRules.plan(new long[0], new int[0], new boolean[0], false).length);
        assertEquals(6, RaftFallRules.CAP);
    }

    @Test
    @DisplayName("deterministic order whatever the input order")
    void deterministic() {
        long[] a = RaftFallRules.plan(new long[] {5, 3, 9}, new int[] {64, 64, 63}, new boolean[3], false);
        long[] b = RaftFallRules.plan(new long[] {9, 5, 3}, new int[] {63, 64, 64}, new boolean[3], false);
        assertArrayEquals(a, b);
        assertArrayEquals(new long[] {9, 3, 5}, a);
    }

    @Test
    @DisplayName("two plans per chunk per tick")
    void perChunk() {
        assertTrue(RaftFallRules.allowed(0));
        assertTrue(RaftFallRules.allowed(1));
        assertFalse(RaftFallRules.allowed(2));
        assertEquals(2, RaftFallRules.CHECK_DELAY);
    }
}
