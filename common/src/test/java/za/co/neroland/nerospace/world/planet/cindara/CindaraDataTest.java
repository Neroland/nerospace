package za.co.neroland.nerospace.world.planet.cindara;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/**
 * Cindara data files. Guards the save-compat rules: the dimension keeps its id and the shared heights on its new
 * dimension type, the 1.6.0 biome stays reachable, and the 1.6.0 Cindrite placement still runs in every biome.
 */
class CindaraDataTest {

    static final List<String> BLOCKS = List.of("ashstone", "ember_cracked_ashstone", "basalt_spire", "magma_glass",
            "ash_layer", "sulfur_crust");
    static final List<String> ITEMS = List.of("ash_dust", "sulfur");
    static final List<String> BIOMES = List.of("cindara", "cindara_ember_plains", "cindara_sulfur_vents");

    private static boolean exists(String path) {
        return CindaraDataTest.class.getResource(path) != null;
    }

    private static JsonElement load(String path) throws IOException {
        try (InputStream in = CindaraDataTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @Test
    void everyIdHasAssetsAndLoot() throws IOException {
        for (String id : BLOCKS) {
            assertTrue(exists("/assets/nerospace/blockstates/" + id + ".json"), "blockstate " + id);
            assertTrue(exists("/assets/nerospace/items/" + id + ".json"), "item model " + id);
            JsonObject loot = load("/data/nerospace/loot_table/blocks/" + id + ".json").getAsJsonObject();
            assertEquals("minecraft:block", loot.get("type").getAsString(), id);
        }
        for (String id : ITEMS) {
            assertTrue(exists("/assets/nerospace/items/" + id + ".json"), "item model " + id);
            assertTrue(exists("/assets/nerospace/textures/item/" + id + ".png"), "texture " + id);
        }
        JsonObject layers = load("/assets/nerospace/blockstates/ash_layer.json").getAsJsonObject()
                .getAsJsonObject("variants");
        for (int i = 1; i <= 8; i++) {
            assertTrue(layers.has("layers=" + i), "ash layer state " + i);
        }
    }

    @Test
    void dimensionMovesToItsOwnTypeWithIdenticalHeights() throws IOException {
        JsonObject space = load("/data/nerospace/dimension_type/space.json").getAsJsonObject();
        JsonObject t = load("/data/nerospace/dimension_type/cindara.json").getAsJsonObject();
        for (String k : List.of("min_y", "height", "logical_height", "coordinate_scale", "has_fixed_time",
                "has_skylight", "ambient_light", "infiniburn")) {
            assertEquals(space.get(k), t.get(k), k + " must match space.json");
        }
        assertEquals(-64, t.get("min_y").getAsInt());
        assertEquals(384, t.get("height").getAsInt());
        JsonObject dim = load("/data/nerospace/dimension/cindara.json").getAsJsonObject();
        assertEquals("nerospace:cindara", dim.get("type").getAsString());
        JsonObject gen = dim.getAsJsonObject("generator");
        assertEquals("minecraft:noise", gen.get("type").getAsString(), "generator type is kept");
        assertEquals("nerospace:cindara", gen.get("settings").getAsString());
        Set<String> biomes = new TreeSet<>();
        for (JsonElement e : gen.getAsJsonObject("biome_source").getAsJsonArray("biomes")) {
            biomes.add(e.getAsJsonObject().get("biome").getAsString());
        }
        Set<String> expected = new TreeSet<>();
        BIOMES.forEach(b -> expected.add("nerospace:" + b));
        assertEquals(expected, biomes, "the 1.6.0 biome nerospace:cindara stays reachable");
    }

    private static final Pattern STATE = Pattern.compile("\"(?:Name|result_state)\"\\s*:\\s*\"([a-z0-9_:]+)\"");

    @Test
    void surfaceIsAshstoneOverStone() throws IOException {
        String text = load("/data/nerospace/worldgen/noise_settings/cindara.json").toString();
        Set<String> placed = new TreeSet<>();
        Matcher m = STATE.matcher(text);
        while (m.find()) {
            placed.add(m.group(1));
        }
        assertTrue(placed.contains("nerospace:ashstone"), "surface uses ashstone: " + placed);
        for (String s : placed) {
            assertTrue(s.startsWith("nerospace:") || Set.of("minecraft:bedrock", "minecraft:deepslate", "minecraft:air",
                    "minecraft:stone").contains(s), "unexpected block " + s);
        }
        // Cindrite targets the stone/deepslate ore replaceables: the body must stay stone for the 1.6.0 rate.
        assertTrue(text.contains("minecraft:stone"), "stone body kept for cindrite");
    }

    @Test
    void everyBiomeKeepsCindriteAndUsesExistingPlacements() throws IOException {
        for (String b : BIOMES) {
            JsonObject biome = load("/data/nerospace/worldgen/biome/" + b + ".json").getAsJsonObject();
            assertFalse(biome.get("has_precipitation").getAsBoolean(), b);
            assertTrue(biome.toString().contains("\"nerospace:cindrite_ore_placed\""), b + " keeps the 1.6.0 ore");
            for (JsonElement step : biome.getAsJsonArray("features")) {
                for (JsonElement f : (JsonArray) step) {
                    String id = f.getAsString();
                    if (!id.startsWith("nerospace:")) {
                        continue; // vanilla placements (lava lakes)
                    }
                    JsonObject placed = load("/data/nerospace/worldgen/placed_feature/" + id.substring(10) + ".json")
                            .getAsJsonObject();
                    String feature = placed.get("feature").getAsString().substring("nerospace:".length());
                    assertTrue(exists("/data/nerospace/worldgen/configured_feature/" + feature + ".json")
                            || exists("/data/nerospace/worldgen/feature/" + feature + ".json"), "feature " + feature);
                }
            }
        }
        JsonObject placed = load("/data/nerospace/worldgen/placed_feature/cindrite_ore_placed.json").getAsJsonObject();
        assertEquals(7, placed.getAsJsonArray("placement").get(0).getAsJsonObject().get("count").getAsInt(),
                "1.6.0 cindrite count unchanged");
        assertTrue(load("/data/nerospace/worldgen/biome/cindara_sulfur_vents.json").toString()
                .contains("nerospace:cindara_basalt_spire_placed"), "spires are a vents feature");
        assertTrue(load("/data/nerospace/worldgen/biome/cindara_ember_plains.json").toString()
                .contains("nerospace:cindara_cindrite_ore_bonus_placed"), "ember plains get the x1.5 cindrite bonus");
    }
}
