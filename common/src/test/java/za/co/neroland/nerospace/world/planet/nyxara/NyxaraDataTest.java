package za.co.neroland.nerospace.world.planet.nyxara;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.nyxara.worldgen.FixedXBands;

/**
 * Nyxara data files: every id has assets and loot; the dimension uses the fixed-X source with the three biomes and the
 * default width; the dimension type keeps the shared heights; every placed feature a biome lists exists.
 */
class NyxaraDataTest {

    static final List<String> BLOCKS = List.of("nyx_stone", "dawnband_stone", "shadow_lichen", "void_crystal",
            "voidglass_ore", "gravity_shard_block");
    static final List<String> ITEMS = List.of("voidglass", "voidglass_lens", "gravity_shard", "night_fibre",
            "void_shard");
    static final List<String> BIOMES = List.of("nyxara_dayside", "nyxara_twilight_band", "nyxara_nightside");

    private static boolean exists(String path) {
        return NyxaraDataTest.class.getResource(path) != null;
    }

    private static JsonObject load(String path) throws IOException {
        try (InputStream in = NyxaraDataTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            JsonElement e = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return e.getAsJsonObject();
        }
    }

    @Test
    void everyIdHasAssetsAndLoot() throws IOException {
        for (String id : BLOCKS) {
            assertTrue(exists("/assets/nerospace/blockstates/" + id + ".json"), "blockstate " + id);
            assertTrue(exists("/assets/nerospace/items/" + id + ".json"), "item model " + id);
            assertTrue(exists("/assets/nerospace/textures/block/" + id + ".png"), "texture " + id);
            assertEquals("minecraft:block",
                    load("/data/nerospace/loot_table/blocks/" + id + ".json").get("type").getAsString(), id);
        }
        for (String id : ITEMS) {
            assertTrue(exists("/assets/nerospace/items/" + id + ".json"), "item model " + id);
            assertTrue(exists("/assets/nerospace/textures/item/" + id + ".png"), "texture " + id);
        }
        assertTrue(exists("/assets/nerospace/textures/block/dawnband_stone.png.mcmeta"), "dawnband animation");
    }

    @Test
    void dimensionUsesFixedX() throws IOException {
        JsonObject dim = load("/data/nerospace/dimension/nyxara.json");
        assertEquals("nerospace:nyxara", dim.get("type").getAsString());
        JsonObject src = dim.getAsJsonObject("generator").getAsJsonObject("biome_source");
        assertEquals("nerospace:fixed_x", src.get("type").getAsString());
        assertEquals("nerospace:nyxara_dayside", src.get("west").getAsString());
        assertEquals("nerospace:nyxara_twilight_band", src.get("band").getAsString());
        assertEquals("nerospace:nyxara_nightside", src.get("east").getAsString());
        assertEquals(FixedXBands.DEFAULT_WIDTH, src.get("band_width").getAsInt());
        assertEquals(0, src.get("origin_x").getAsInt());
        assertEquals(FixedXBands.DEFAULT_EDGE_NOISE, src.get("edge_noise").getAsInt());
        assertEquals("nerospace:nyxara", dim.getAsJsonObject("generator").get("settings").getAsString());
    }

    @Test
    void dimensionTypeKeepsSharedHeights() throws IOException {
        JsonObject t = load("/data/nerospace/dimension_type/nyxara.json");
        JsonObject space = load("/data/nerospace/dimension_type/space.json");
        for (String k : List.of("min_y", "height", "logical_height", "coordinate_scale", "has_fixed_time")) {
            assertEquals(space.get(k), t.get(k), k);
        }
        assertEquals(-64, t.get("min_y").getAsInt());
        assertEquals(384, t.get("height").getAsInt());
    }

    @Test
    void biomeFeaturesExist() throws IOException {
        for (String b : BIOMES) {
            JsonObject biome = load("/data/nerospace/worldgen/biome/" + b + ".json");
            for (JsonElement step : biome.getAsJsonArray("features")) {
                for (JsonElement f : step.getAsJsonArray()) {
                    String id = f.getAsString();
                    assertTrue(id.startsWith("nerospace:"), id);
                    assertTrue(exists("/data/nerospace/worldgen/placed_feature/" + id.substring(10) + ".json"),
                            b + " lists missing " + id);
                }
            }
            assertTrue(biome.getAsJsonArray("features").get(6).toString().contains("nerospace:nerosium_ore_placed"),
                    b + " keeps nerosium");
        }
    }
}
