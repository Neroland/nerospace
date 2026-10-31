package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Smoke Crystal. */
@SuppressWarnings("unused")
class HushRulesTest {

    @Test
    @DisplayName("steps and falls are silent with vanilla intentionally_empty; break and place are not")
    void silent() {
        assertEquals("minecraft:intentionally_empty", HushRules.SILENT_EVENT);
        assertTrue(HushRules.silent(true));
        assertFalse(HushRules.silent(false));
        assertEquals(1, HushRules.LIGHT);
    }

    @Test
    @DisplayName("smoke crystal is in both vibration tags")
    void vibrationTags() throws IOException {
        for (String tag : HushRules.VIBRATION_TAGS) {
            String path = "/data/minecraft/tags/block/" + tag.substring("minecraft:".length()) + ".json";
            try (InputStream in = HushRulesTest.class.getResourceAsStream(path)) {
                assertNotNull(in, "missing " + path);
                boolean found = false;
                for (JsonElement e : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .getAsJsonObject().getAsJsonArray("values")) {
                    found |= e.getAsString().equals("nerospace:smoke_crystal");
                }
                assertTrue(found, tag);
            }
        }
    }
}
