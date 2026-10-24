package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §5 Cheese — 9 in, 9 out. */
@SuppressWarnings("unused")
class CheeseRulesTest {

    @Test
    @DisplayName("slicing all the way and breaking at any point both return 9")
    void conservation() {
        for (int start = 0; start <= CheeseRules.MAX_BITES; start++) {
            int sliced = 0;
            int bites = start;
            while (bites >= 0) {
                sliced++;
                bites = CheeseRules.afterSlice(bites);
            }
            assertEquals(CheeseRules.SLICES, start + sliced, "slices from bites " + start);
            assertEquals(CheeseRules.SLICES - start, CheeseRules.breakDrops(start));
        }
        assertEquals(-1, CheeseRules.afterSlice(8), "ninth slice removes the block");
    }

    @Test
    @DisplayName("ore 1–2, fortune up to +2; food numbers")
    void ore() {
        assertEquals(1, CheeseRules.oreDrops(0, 0));
        assertEquals(2, CheeseRules.oreDrops(5, 0));
        assertEquals(4, CheeseRules.oreDrops(2, 9));
        assertEquals(4, CheeseRules.NUTRITION);
        assertEquals(0.6F, CheeseRules.SATURATION);
        assertEquals(400, CheeseRules.REGEN_TICKS);
    }
}
