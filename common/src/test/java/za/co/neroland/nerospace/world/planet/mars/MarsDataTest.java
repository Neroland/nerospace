package za.co.neroland.nerospace.world.planet.mars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

import za.co.neroland.nerospace.block.rules.StrataRules;

/** Mars data files: every id has its assets and loot, the dimension keeps the shared heights, worldgen uses Mars blocks. */
class MarsDataTest {

    static final List<String> BLOCKS = blocks();
    static final List<String> ITEMS = List.of("raw_hematite", "rust_pigment", "perchlorate_oxidiser");
    static final List<String> BIOMES = List.of("mars_dunes", "mars_canyons", "mars_polar");

    private static List<String> blocks() {
        List<String> out = new ArrayList<>(List.of("red_regolith", "martian_sandstone"));
        for (int i = 0; i < StrataRules.COUNT; i++) {
            out.add(StrataRules.variantFor(i));
        }
        out.addAll(List.of("basalt_column", "dust_devil_pit", "dry_ice", "hematite_ore", "perchlorate_crust"));
        return List.copyOf(out);
    }

    private static boolean exists(String path) {
        return MarsDataTest.class.getResource(path) != null;
    }

    private static JsonElement load(String path) throws IOException {
        try (InputStream in = MarsDataTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            // The 1.21.1 build rewrites short-form ingredients ("id" / "#tag") into {"item": ..} / {"tag": ..}
            // objects; fold them back so the checks read one format on every version.
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\{\\s*\"item\"\\s*:\\s*(\"[^\"]+\")\\s*}", "$1")
                    .replaceAll("\\{\\s*\"tag\"\\s*:\\s*\"([^\"]+)\"\\s*}", "\"#$1\"");
            return JsonParser.parseString(json);
        }
    }

    @Test
    void everyBlockHasStateModelItemAndLoot() throws IOException {
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
    }

    @Test
    void strataAreStonecutFromSandstone() throws IOException {
        for (int i = 0; i < StrataRules.COUNT; i++) {
            String id = StrataRules.variantFor(i);
            JsonObject r = load("/data/nerospace/recipe/" + id + ".json").getAsJsonObject();
            assertEquals("minecraft:stonecutting", r.get("type").getAsString());
            assertEquals("nerospace:martian_sandstone", r.get("ingredient").getAsString());
            assertEquals("nerospace:" + id, r.getAsJsonObject("result").get("id").getAsString());
            assertTrue(exists("/assets/nerospace/textures/block/" + id + ".png"), "texture " + id);
        }
    }

    @Test
    void dimensionTypeKeepsSharedHeights() throws IOException {
        JsonObject t = load("/data/nerospace/dimension_type/mars.json").getAsJsonObject();
        assertEquals(-64, t.get("min_y").getAsInt());
        assertEquals(384, t.get("height").getAsInt());
        assertEquals(384, t.get("logical_height").getAsInt());
        assertEquals(1.0, t.get("coordinate_scale").getAsDouble());
        //? if >=1.21.11 {
        assertTrue(t.get("has_fixed_time").getAsBoolean(), "the planet clock drives the sol");
        //?} else {
        /*assertTrue(t.has("fixed_time"), "the planet clock drives the sol"); // the build's pre-1.21.11 form
        *///?}
        JsonObject dim = load("/data/nerospace/dimension/mars.json").getAsJsonObject();
        assertEquals("nerospace:mars", dim.get("type").getAsString());
        JsonObject gen = dim.getAsJsonObject("generator");
        assertEquals("nerospace:mars", gen.get("settings").getAsString());
        Set<String> biomes = new TreeSet<>();
        for (JsonElement e : gen.getAsJsonObject("biome_source").getAsJsonArray("biomes")) {
            biomes.add(e.getAsJsonObject().get("biome").getAsString());
        }
        Set<String> expected = new TreeSet<>();
        BIOMES.forEach(b -> expected.add("nerospace:" + b));
        assertEquals(expected, biomes);
    }

    private static final Pattern STATE = Pattern.compile("\"(?:Name|result_state)\"\\s*:\\s*\"([a-z0-9_:]+)\"");

    @Test
    void surfaceIsBuiltFromMarsBlocks() throws IOException {
        String text = load("/data/nerospace/worldgen/noise_settings/mars.json").toString();
        Set<String> placed = new TreeSet<>();
        Matcher m = STATE.matcher(text);
        while (m.find()) {
            placed.add(m.group(1));
        }
        for (String id : List.of("red_regolith", "martian_sandstone", "dry_ice", StrataRules.variantFor(0),
                StrataRules.variantFor(3))) {
            assertTrue(placed.contains("nerospace:" + id), "surface uses " + id + " in " + placed);
        }
        for (String s : placed) {
            assertTrue(s.startsWith("nerospace:") || Set.of("minecraft:bedrock", "minecraft:deepslate", "minecraft:air",
                    "minecraft:stone").contains(s), "unexpected surface block " + s);
        }
        assertFalse(placed.contains("minecraft:water"), "Mars has no open water before terraforming");
    }

    @Test
    void biomesUsePlacedFeaturesThatExist() throws IOException {
        for (String b : BIOMES) {
            JsonObject biome = load("/data/nerospace/worldgen/biome/" + b + ".json").getAsJsonObject();
            assertFalse(biome.get("has_precipitation").getAsBoolean(), b);
            for (JsonElement step : biome.getAsJsonArray("features")) {
                for (JsonElement f : (JsonArray) step) {
                    String path = f.getAsString().substring("nerospace:".length());
                    JsonObject placed = load("/data/nerospace/worldgen/placed_feature/" + path + ".json").getAsJsonObject();
                    String feature = placed.get("feature").getAsString().substring("nerospace:".length());
                    assertTrue(exists("/data/nerospace/worldgen/configured_feature/" + feature + ".json")
                            || exists("/data/nerospace/worldgen/feature/" + feature + ".json"), "feature " + feature);
                }
            }
        }
        String dunes = load("/data/nerospace/worldgen/biome/mars_dunes.json").toString();
        assertTrue(dunes.contains("nerospace:mars_dust_devil_pit_placed"), "pits are a dunes feature");
        assertFalse(load("/data/nerospace/worldgen/biome/mars_polar.json").toString().contains("dust_devil_pit"));
    }
}
