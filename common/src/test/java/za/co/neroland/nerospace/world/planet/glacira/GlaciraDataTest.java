package za.co.neroland.nerospace.world.planet.glacira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/**
 * Glacira data files: every new id has its assets and loot; the new dimension type keeps the 1.x heights of
 * {@code nerospace:space}; the kept {@code nerospace:glacira} biome stays reachable and every Glacira biome keeps the
 * 1.x glacite placement (spires add the bonus on top).
 */
class GlaciraDataTest {

    static final List<String> BLOCKS = List.of("permafrost", "packed_cryo_ice", "aurora_stone", "frozen_geyser",
            "frost_crystal_cluster", "methane_ice");
    static final List<String> ITEMS = List.of("frost_shard", "methane_crystal");
    static final List<String> BIOMES = List.of("glacira", "glacira_methane_shore", "glacira_spires");

    private static boolean exists(String path) {
        return GlaciraDataTest.class.getResource(path) != null;
    }

    private static JsonObject load(String path) throws IOException {
        try (InputStream in = GlaciraDataTest.class.getResourceAsStream(path)) {
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
            assertEquals("minecraft:block",
                    load("/data/nerospace/loot_table/blocks/" + id + ".json").get("type").getAsString(), id);
        }
        for (String id : ITEMS) {
            assertTrue(exists("/assets/nerospace/items/" + id + ".json"), "item model " + id);
            assertTrue(exists("/assets/nerospace/textures/item/" + id + ".png"), "texture " + id);
        }
        for (int s = 0; s < 4; s++) {
            assertTrue(exists("/assets/nerospace/textures/block/frost_crystal_cluster_stage" + s + ".png"));
        }
    }

    @Test
    void dimensionTypeKeepsTheOldHeights() throws IOException {
        JsonObject space = load("/data/nerospace/dimension_type/space.json");
        JsonObject glacira = load("/data/nerospace/dimension_type/glacira.json");
        for (String k : List.of("min_y", "height", "logical_height", "coordinate_scale", "has_fixed_time",
                "has_skylight", "has_ceiling")) {
            assertEquals(space.get(k), glacira.get(k), k);
        }
        assertEquals(-64, glacira.get("min_y").getAsInt());
        assertEquals(384, glacira.get("height").getAsInt());
    }

    @Test
    void dimensionUsesTheNewTypeAndKeepsTheOldBiomeReachable() throws IOException {
        JsonObject dim = load("/data/nerospace/dimension/glacira.json");
        assertEquals("nerospace:glacira", dim.get("type").getAsString());
        JsonObject gen = dim.getAsJsonObject("generator");
        assertEquals("nerospace:glacira", gen.get("settings").getAsString());
        Set<String> biomes = new HashSet<>();
        for (JsonElement e : gen.getAsJsonObject("biome_source").getAsJsonArray("biomes")) {
            biomes.add(e.getAsJsonObject().get("biome").getAsString());
        }
        for (String b : BIOMES) {
            assertTrue(biomes.contains("nerospace:" + b), b);
        }
    }

    @Test
    void everyBiomeKeepsTheGlaciteOre() throws IOException {
        for (String b : BIOMES) {
            JsonArray ores = load("/data/nerospace/worldgen/biome/" + b + ".json").getAsJsonArray("features")
                    .get(6).getAsJsonArray();
            assertTrue(ores.contains(new com.google.gson.JsonPrimitive("nerospace:glacite_ore_placed")), b);
        }
        JsonArray spires = load("/data/nerospace/worldgen/biome/glacira_spires.json").getAsJsonArray("features")
                .get(6).getAsJsonArray();
        assertTrue(spires.contains(new com.google.gson.JsonPrimitive("nerospace:glacira_glacite_ore_bonus_placed")));
        // 1.x rate unchanged: count 7, size 8, trapezoid -48..48.
        JsonObject placed = load("/data/nerospace/worldgen/placed_feature/glacite_ore_placed.json");
        assertEquals(7, placed.getAsJsonArray("placement").get(0).getAsJsonObject().get("count").getAsInt());
    }

    /**
     * Spires are keyed on weirdness (the ridge noise, a few hundred blocks per cycle) rather than low erosion (a
     * continent-scale noise that left the nearest spires ~8 000 blocks from spawn), so all three biomes appear near
     * the origin. Plains keep the widest slice.
     */
    @Test
    void spiresAreKeyedOnRidgesNearSpawn() throws IOException {
        JsonArray biomes = load("/data/nerospace/dimension/glacira.json").getAsJsonObject("generator")
                .getAsJsonObject("biome_source").getAsJsonArray("biomes");
        double spiresWeird = 0;
        double plainsWeird = 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (JsonElement e : biomes) {
            JsonObject o = e.getAsJsonObject();
            String b = o.get("biome").getAsString();
            seen.add(b);
            JsonObject p = o.getAsJsonObject("parameters");
            JsonArray w = p.getAsJsonArray("weirdness");
            double span = w.get(1).getAsDouble() - w.get(0).getAsDouble();
            if (b.equals("nerospace:glacira_spires")) {
                JsonArray er = p.getAsJsonArray("erosion");
                assertEquals(-1.0, er.get(0).getAsDouble(), 1.0E-9, "spires span all erosion");
                assertEquals(1.0, er.get(1).getAsDouble(), 1.0E-9, "spires span all erosion");
                spiresWeird += span;
            } else if (b.equals("nerospace:glacira")) {
                plainsWeird += span;
            }
        }
        for (String b : BIOMES) {
            assertTrue(seen.contains("nerospace:" + b), b);
        }
        assertTrue(spiresWeird > 0 && plainsWeird > spiresWeird, "plains keep the widest weirdness slice");
    }

    @Test
    void surfaceIsBuiltFromGlaciraBlocks() throws IOException {
        String noise = load("/data/nerospace/worldgen/noise_settings/glacira.json").toString();
        assertTrue(noise.contains("nerospace:permafrost"));
        assertTrue(noise.contains("nerospace:packed_cryo_ice"));
        assertTrue(noise.contains("nerospace:glacira_spires"));
        assertTrue(noise.contains("nerospace:glacira_methane_shore"));
    }
}
