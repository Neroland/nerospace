package za.co.neroland.nerospace.block.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §3 light absorb index: the 3×3×3 neighbourhood bitset behind the Nyx Stone mixin. */
class NyxLightIndexTest {

    @Test
    @DisplayName("empty index answers false with no work")
    void empty() {
        NyxLightIndex index = new NyxLightIndex();
        assertTrue(index.isEmpty());
        assertFalse(index.near(0, 64, 0));
    }

    @Test
    @DisplayName("near covers exactly the 3x3x3 around each absorber, across section and chunk borders")
    void neighbourhood() {
        NyxLightIndex index = new NyxLightIndex();
        assertTrue(index.add(15, 15, -1)); // corner of a section, negative z chunk
        assertFalse(index.add(15, 15, -1), "idempotent");
        int hits = 0;
        for (int x = 12; x <= 18; x++) {
            for (int y = 12; y <= 18; y++) {
                for (int z = -4; z <= 2; z++) {
                    if (index.near(x, y, z)) {
                        hits++;
                        assertTrue(Math.abs(x - 15) <= 1 && Math.abs(y - 15) <= 1 && Math.abs(z + 1) <= 1);
                    }
                }
            }
        }
        assertEquals(27, hits);
    }

    @Test
    @DisplayName("overlapping halos count; removing one keeps the other's cells")
    void overlap() {
        NyxLightIndex index = new NyxLightIndex();
        index.add(0, -60, 0);
        index.add(2, -60, 0);
        assertTrue(index.near(1, -60, 0));
        assertTrue(index.remove(0, -60, 0));
        assertFalse(index.remove(0, -60, 0));
        assertTrue(index.near(1, -60, 0), "still near the second absorber");
        assertFalse(index.near(-1, -60, 0));
        index.remove(2, -60, 0);
        assertFalse(index.near(1, -60, 0));
        assertTrue(index.isEmpty());
    }

    @Test
    @DisplayName("clearChunk drops only that column's absorbers and frees memory")
    void clearChunk() {
        NyxLightIndex index = new NyxLightIndex();
        index.add(5, 70, 5);      // chunk (0,0)
        index.add(-20, 70, 5);    // chunk (-2,0)
        index.clearChunk(0, 0);
        assertEquals(1, index.size());
        assertFalse(index.near(5, 70, 5));
        assertTrue(index.near(-20, 70, 5));
        index.clearChunk(-2, 0);
        assertTrue(index.isEmpty());
        assertEquals(0, index.sectionsHeld());
    }

    @Test
    @DisplayName("pack round-trips the full block range")
    void pack() {
        int[][] cases = {{0, 0, 0}, {-30_000_000, -2048, 29_999_999}, {29_999_999, 2047, -30_000_000}, {-1, -1, -1}};
        for (int[] c : cases) {
            long p = NyxLightIndex.pack(c[0], c[1], c[2]);
            assertEquals(c[0], NyxLightIndex.unpackX(p));
            assertEquals(c[1], NyxLightIndex.unpackY(p));
            assertEquals(c[2], NyxLightIndex.unpackZ(p));
        }
    }
}
