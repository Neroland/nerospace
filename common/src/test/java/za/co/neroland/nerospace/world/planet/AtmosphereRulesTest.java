package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Terraformed ground is breathable everywhere except on a world whose table says it never gains an atmosphere. */
class AtmosphereRulesTest {

    @Test
    @DisplayName("every table but Europa's keeps the old rule: terraformed means breathable")
    void existingTablesUnchanged() {
        for (TerraformTable t : TerraformTables.ORDER) {
            if (t.langId().equals("europa")) {
                continue;
            }
            boolean no = t.has(TerraformTable.Rule.NO_ATMOSPHERE);
            assertFalse(no, t.langId());
            for (boolean terraformed : new boolean[] {false, true}) {
                assertEquals(terraformed, AtmosphereRules.terraformBreathable(no, terraformed), t.langId());
            }
        }
    }

    @Test
    @DisplayName("Europa: never breathable through terraforming")
    void europa() {
        TerraformTable t = TerraformTables.forLangId("europa").orElseThrow();
        assertTrue(t.has(TerraformTable.Rule.NO_ATMOSPHERE));
        assertFalse(AtmosphereRules.terraformBreathable(true, true));
        assertFalse(AtmosphereRules.terraformBreathable(true, false));
    }
}
