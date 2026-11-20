package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The open-ocean column rule: how many cells open, and which loose cover is looked through. */
class OpenOceanRulesTest {

    @Test
    @DisplayName("cells: 0 unless the top is the surface top; then 1 plus the convertible run, at most 4")
    void cells() {
        assertEquals(3, OpenOceanRules.DEPTH);
        assertEquals(4, OpenOceanRules.MAX_CELLS);
        assertEquals(0, OpenOceanRules.cells(false, 3));
        assertEquals(1, OpenOceanRules.cells(true, 0));
        assertEquals(3, OpenOceanRules.cells(true, 2));
        assertEquals(4, OpenOceanRules.cells(true, 3));
        assertEquals(4, OpenOceanRules.cells(true, 40));
        assertEquals(1, OpenOceanRules.cells(true, -2));
    }

    @Test
    @DisplayName("look-through: blades and snow layers only; a vent or an ore on top leaves the column alone")
    void lookThrough() {
        assertTrue(OpenOceanRules.lookThrough("nerospace:penitente_blade"));
        assertTrue(OpenOceanRules.lookThrough("minecraft:snow"));
        assertFalse(OpenOceanRules.lookThrough("nerospace:cryovent"));
        assertFalse(OpenOceanRules.lookThrough("nerospace:oxidant_ice"));
        assertFalse(OpenOceanRules.lookThrough("minecraft:snow_block"));
        assertEquals(0, OpenOceanRules.topIndex(List.of("minecraft:snow_block", "nerospace:europa_ice")));
        assertEquals(2, OpenOceanRules.topIndex(List.of("minecraft:snow", "nerospace:penitente_blade",
                "minecraft:snow_block")));
        assertEquals(0, OpenOceanRules.topIndex(List.of("nerospace:cryovent", "minecraft:snow_block")));
        assertEquals(-1, OpenOceanRules.topIndex(List.of("minecraft:snow")));
        assertEquals(-1, OpenOceanRules.topIndex(List.of()));
    }

    @Test
    @DisplayName("only Europa's table has the rule, after the spring rule and before the atmosphere rule")
    void onlyEuropa() {
        for (TerraformTable t : TerraformTables.ORDER) {
            assertEquals(t.langId().equals("europa"), t.has(TerraformTable.Rule.OPEN_OCEAN), t.langId());
        }
        assertEquals(List.of(TerraformTable.Rule.OPEN_WATER_SPRING, TerraformTable.Rule.OPEN_OCEAN,
                TerraformTable.Rule.NO_ATMOSPHERE), TerraformTables.forLangId("europa").orElseThrow().rules());
    }
}
