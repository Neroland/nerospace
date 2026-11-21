package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The biome-temperature guard on weather freezing: Glacira still freezes everywhere, a terraformed sea stays open. */
class FreezeGuardRulesTest {

    private static float temperature(String biome) throws IOException {
        try (InputStream in = FreezeGuardRulesTest.class
                .getResourceAsStream("/data/nerospace/worldgen/biome/" + biome + ".json")) {
            assertTrue(in != null, biome);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                    .get("temperature").getAsFloat();
        }
    }

    @Test
    @DisplayName("the threshold is vanilla's: below 0.15 freezes")
    void threshold() {
        assertTrue(FreezeGuardRules.biomeFreezes(0.0F));
        assertTrue(FreezeGuardRules.biomeFreezes(0.149F));
        assertFalse(FreezeGuardRules.biomeFreezes(0.15F));
        assertFalse(FreezeGuardRules.biomeFreezes(0.2F));
        assertFalse(FreezeGuardRules.biomeFreezes(0.35F));
        assertEquals(0.15F, FreezeGuardRules.FREEZE_BELOW);
    }

    /**
     * Mirrors vanilla's height-adjusted temperature (read from the 1.21.1 and 26.3 {@code Biome} sources):
     * 1.21.x drops {@code (noise*8 + y - 80) * 0.05/40} above Y 80; 26.x the same above {@code seaLevel + 17};
     * freezing below 0.15. Europa's sea level is 88, its surface Y 176..212.
     */
    @Test
    @DisplayName("vanilla height rule: 0.35 stays open at Y 176 and 212 on both formulas; the old 0.2 froze")
    void vanillaHeightRule() {
        assertEquals(0.35F, FreezeGuardRules.vanillaTemperature(0.35F, 80, 88, true, 1.0F), 1e-6);
        assertEquals(0.35F, FreezeGuardRules.vanillaTemperature(0.35F, 105, 88, false, 1.0F), 1e-6);
        assertEquals(0.35F - (8 + 212 - 80) * 0.05F / 40.0F,
                FreezeGuardRules.vanillaTemperature(0.35F, 212, 88, true, 1.0F), 1e-6);
        assertEquals(0.35F - (8 + 212 - 105) * 0.05F / 40.0F,
                FreezeGuardRules.vanillaTemperature(0.35F, 212, 88, false, 1.0F), 1e-6);
        for (int y : new int[] {176, 212}) {
            for (boolean legacy : new boolean[] {true, false}) {
                assertFalse(FreezeGuardRules.vanillaFreezes(0.35F, y, 88, legacy), "0.35 y " + y + " " + legacy);
                assertTrue(FreezeGuardRules.vanillaFreezes(0.2F, y, 88, legacy), "0.2 y " + y + " " + legacy);
            }
        }
        // Minimum bases: 0.325 (1.21.x) and 0.29375 (26.x) at Y 212.
        assertFalse(FreezeGuardRules.vanillaFreezes(0.33F, 212, 88, true));
        assertTrue(FreezeGuardRules.vanillaFreezes(0.32F, 212, 88, true));
    }

    @Test
    @DisplayName("Glacira: every natural and terraformed biome still freezes (behaviour unchanged)")
    void glaciraUnchanged() throws IOException {
        for (String b : List.of("glacira", "glacira_methane_shore", "glacira_spires", "terraformed_glacira_rooted",
                "terraformed_glacira_hydrated", "terraformed_tundra")) {
            assertTrue(FreezeGuardRules.biomeFreezes(temperature(b)), b);
        }
    }

    @Test
    @DisplayName("Europa: natural ice and the Rooted stage freeze; the Hydrated and Living sea stays open")
    void europa() throws IOException {
        for (String b : List.of("europa_ice_plains", "europa_chaos", "europa_lineae", "europa_penitentes",
                "europa_shell", "europa_abyss", "europa_vent_fields", "terraformed_europa_rooted")) {
            assertTrue(FreezeGuardRules.biomeFreezes(temperature(b)), b);
        }
        assertFalse(FreezeGuardRules.biomeFreezes(temperature("terraformed_europa_hydrated")));
        assertFalse(FreezeGuardRules.biomeFreezes(temperature("terraformed_europa_living")));
    }
}
