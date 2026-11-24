package za.co.neroland.nerospace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

final class LegacyDatapackTest {
    @Test
    void lunaUsesTheUniformProviderShapeSupportedByItsGameVersion() throws IOException {
        JsonObject dimension = read("/data/nerospace/dimension_type/luna.json");
        JsonObject provider = dimension.getAsJsonObject("monster_spawn_light_level");
        assertEquals("minecraft:uniform", provider.get("type").getAsString());

        if (Boolean.getBoolean("nerospace.nbtItems")) {
            assertFalse(provider.has("min_inclusive"));
            provider = provider.getAsJsonObject("value");
        } else {
            assertFalse(provider.has("value"));
        }
        assertEquals(0, provider.get("min_inclusive").getAsInt());
        assertEquals(7, provider.get("max_inclusive").getAsInt());
    }

    @Test
    void shadowLichenStillRequiresShears() throws IOException {
        JsonObject loot = read(TestResourcePaths.datapack("/data/nerospace/loot_table/blocks/shadow_lichen.json"));
        JsonObject drop = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject();
        if (drop.has("condition")) {
            assertEquals("minecraft:tool/can_shear", drop.get("condition").getAsString());
            return;
        }
        JsonObject predicate = drop.getAsJsonArray("conditions").get(0).getAsJsonObject().getAsJsonObject("predicate");
        if (Boolean.getBoolean("nerospace.nbtItems")) {
            assertEquals(1, predicate.getAsJsonArray("items").size());
            assertEquals("minecraft:shears", predicate.getAsJsonArray("items").get(0).getAsString());
        } else {
            assertEquals("minecraft:shears", predicate.get("items").getAsString());
        }
    }

    private JsonObject read(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }
}
