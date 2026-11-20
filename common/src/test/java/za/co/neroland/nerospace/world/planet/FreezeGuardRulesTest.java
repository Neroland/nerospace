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
        assertEquals(0.15F, FreezeGuardRules.FREEZE_BELOW);
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
