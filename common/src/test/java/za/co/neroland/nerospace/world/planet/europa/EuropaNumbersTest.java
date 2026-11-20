package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.block.rules.GripRules;
import za.co.neroland.nerospace.machine.GeneratorFuels;
import za.co.neroland.nerospace.machine.RefineryRules;

/** Europa's tech-chain and block numbers, against the machines that use them. */
class EuropaNumbersTest {

    @Test
    @DisplayName("a deuterium cell burns 8,000 ticks: twice a rocket fuel canister")
    void cell() {
        assertEquals(8_000, EuropaNumbers.DEUTERIUM_CELL_BURN_TICKS);
        assertEquals(EuropaNumbers.DEUTERIUM_CELL_BURN_TICKS, GeneratorFuels.DEUTERIUM_CELL);
        assertTrue(GeneratorFuels.DEUTERIUM_CELL > GeneratorFuels.ROCKET_FUEL_CANISTER);
        assertEquals(2 * GeneratorFuels.ROCKET_FUEL_CANISTER, GeneratorFuels.DEUTERIUM_CELL);
    }

    @Test
    @DisplayName("the fuels from before 2.4.0 burn as long as they did")
    void olderFuels() {
        assertEquals(1_600, GeneratorFuels.COAL);
        assertEquals(16_000, GeneratorFuels.COAL_BLOCK);
        assertEquals(2_400, GeneratorFuels.BLAZE_ROD);
        assertEquals(4_000, GeneratorFuels.ROCKET_FUEL_CANISTER);
        assertEquals(2_400, GeneratorFuels.METHANE_CRYSTAL);
    }

    @Test
    @DisplayName("cut ice is registered with the grip rule's speed factor")
    void cutIce() {
        assertEquals(1.15F, EuropaNumbers.CUT_ICE_SPEED);
        assertEquals(EuropaNumbers.CUT_ICE_SPEED, GripRules.CUT_SPEED);
        assertEquals(EuropaNumbers.CUT_ICE_SPEED, GripRules.speedFactor(true));
    }

    @Test
    @DisplayName("an oxidant crystal gives 400 mB of oxygen, more than raw ilmenite")
    void crystal() {
        assertEquals(400, EuropaNumbers.OXIDANT_OXYGEN_MB);
        assertEquals(EuropaNumbers.OXIDANT_OXYGEN_MB, RefineryRules.OXIDANT_OXYGEN_MB);
        assertTrue(RefineryRules.OXIDANT_OXYGEN_MB > RefineryRules.ILMENITE_OXYGEN_MB);
    }
}
