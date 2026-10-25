package za.co.neroland.nerospace.world.planet.luna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.block.rules.CheeseRules;
import za.co.neroland.nerospace.block.rules.GasPocketRules;

/** Luna block wiring numbers: He-3 loot + code yield, Cheese Wheel slice geometry. */
class LunaNumbersTest {

    @Test
    @DisplayName("He-3: loot yield + code bonus equals GasPocketRules.yield for every case")
    void he3Total() {
        for (boolean silk : new boolean[] {false, true}) {
            for (boolean tank : new boolean[] {false, true}) {
                int total = LunaNumbers.HE3_LOOT_YIELD + LunaNumbers.he3ExtraYield(silk, tank);
                assertEquals(GasPocketRules.yield(silk, tank, GasPocketRules.BASE_YIELD), total, silk + "/" + tank);
            }
        }
        assertEquals(1, LunaNumbers.HE3_LOOT_YIELD + LunaNumbers.he3ExtraYield(false, false), "leak halves");
        assertEquals(2, LunaNumbers.HE3_LOOT_YIELD + LunaNumbers.he3ExtraYield(true, false), "silk keeps 2");
        assertEquals(2, LunaNumbers.HE3_LOOT_YIELD + LunaNumbers.he3ExtraYield(false, true), "gas tank keeps 2");
    }

    @Test
    @DisplayName("Cheese Wheel: 12/16 wide at 0 bites, shrinks every slice, never empty before the last")
    void wheel() {
        assertEquals(2, LunaNumbers.wheelMinX(0));
        for (int b = 1; b <= CheeseRules.MAX_BITES; b++) {
            assertTrue(LunaNumbers.wheelMinX(b) > LunaNumbers.wheelMinX(b - 1), "bites " + b);
            assertTrue(LunaNumbers.wheelMinX(b) < 14, "bites " + b);
        }
        assertEquals(LunaNumbers.wheelMinX(CheeseRules.MAX_BITES), LunaNumbers.wheelMinX(99), "clamped");
    }
}
