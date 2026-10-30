package za.co.neroland.nerospace.world.planet.greenxertz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Greenxertz data files: every new id has its assets and loot (both loader trees); the kept {@code nerospace:greenxertz}
 * biome stays reachable; all three biomes keep the 1.x step-6 list (ores, hamlet, ruin, mega city) in its 1.x order with
 * the 1.x ore rates; new terrain is built from Greenxertz blocks.
 */
class GreenxertzDataTest {

    static final List<String> BLOCKS = List.of("xertz_soil", "bio_bark_log", "stripped_bio_bark_log", "xertz_vine",
            "spore_moss", "glowcap_fungus", "glowcap_block", "spore_pod", "amber_sap_ore");
    static final List<String> ITEMS = List.of("amber_sap", "xertz_fibre", "spore_bomb");
    static final List<String> BIOMES = List.of("greenxertz", "greenxertz_fungal_depths", "greenxertz_spore_marsh");
    static final List<String> FEATURES = List.of("greenxertz_amber_sap_ore", "greenxertz_spore_moss",
            "greenxertz_glowcap", "greenxertz_spore_pod", "greenxertz_giant_glowcap");
    /** The 1.6.0 Greenxertz step-6 list, in order. New placements may only follow it. */
    static final List<String> KEPT_STEP6 = List.of("nerospace:nerosteel_ore_placed",
            "nerospace:xertz_quartz_ore_placed", "nerospace:hamlet_placed", "nerospace:ruin_placed",
            "nerospace:mega_city_placed");

    private static boolean exists(String path) {
        return GreenxertzDataTest.class.getResource(path) != null;
    }

    private static JsonObject load(String path) throws IOException {
        try (InputStream in = GreenxertzDataTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            JsonElement e = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return e.getAsJsonObject();
        }
    }

    private static Path commonMain() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("common/src/main"))) {
                return p.resolve("common/src/main");
            }
        }
        return null;
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
        assertTrue(exists("/data/nerospace/recipe/spore_bomb.json"));
    }

    @Test
    void bothLoaderTreesCarryLootAndFeatures() {
        Path main = commonMain();
        Assumptions.assumeTrue(main != null, "common/src/main not found");
        List<String> missing = new ArrayList<>();
        for (String id : BLOCKS) {
            for (String tree : List.of("resources", "resources-26.3")) {
                if (!Files.exists(main.resolve(tree + "/data/nerospace/loot_table/blocks/" + id + ".json"))) {
                    missing.add(tree + " loot " + id);
                }
            }
        }
        for (String f : FEATURES) {
            if (!Files.exists(main.resolve("resources/data/nerospace/worldgen/configured_feature/" + f + ".json"))) {
                missing.add("configured_feature " + f);
            }
            if (!Files.exists(main.resolve("resources-26.3/data/nerospace/worldgen/feature/" + f + ".json"))) {
                missing.add("26.3 feature " + f);
            }
        }
        assertTrue(missing.isEmpty(), missing.toString());
    }

    @Test
    void dimensionKeepsTheOldBiomeReachable() throws IOException {
        JsonObject gen = load("/data/nerospace/dimension/greenxertz.json").getAsJsonObject("generator");
        assertEquals("minecraft:noise", gen.get("type").getAsString());
        assertEquals("nerospace:greenxertz", gen.get("settings").getAsString());
        Set<String> biomes = new HashSet<>();
        for (JsonElement e : gen.getAsJsonObject("biome_source").getAsJsonArray("biomes")) {
            biomes.add(e.getAsJsonObject().get("biome").getAsString());
        }
        for (String b : BIOMES) {
            assertTrue(biomes.contains("nerospace:" + b), b);
        }
    }

    @Test
    void everyBiomeKeepsThe1xStructuresAndOresFirstInOrder() throws IOException {
        for (String b : BIOMES) {
            JsonArray step6 = load("/data/nerospace/worldgen/biome/" + b + ".json").getAsJsonArray("features")
                    .get(6).getAsJsonArray();
            for (int i = 0; i < KEPT_STEP6.size(); i++) {
                assertEquals(KEPT_STEP6.get(i), step6.get(i).getAsString(), b + " step 6 index " + i);
            }
        }
        // 1.x rates unchanged: xertz quartz 12 x size 14, nerosteel 10.
        assertEquals(12, load("/data/nerospace/worldgen/placed_feature/xertz_quartz_ore_placed.json")
                .getAsJsonArray("placement").get(0).getAsJsonObject().get("count").getAsInt());
        assertEquals(10, load("/data/nerospace/worldgen/placed_feature/nerosteel_ore_placed.json")
                .getAsJsonArray("placement").get(0).getAsJsonObject().get("count").getAsInt());
    }

    @Test
    void everyBiomeKeepsTheGreenxertzSpawns() throws IOException {
        for (String b : BIOMES) {
            JsonObject biome = load("/data/nerospace/worldgen/biome/" + b + ".json");
            // 26.1.2 / 26.2 read top-level "spawners"; the 26.3 overlay keeps them in the natural_mob_spawns attribute.
            String spawners = (biome.has("spawners") ? biome.getAsJsonObject("spawners")
                    : biome.getAsJsonObject("attributes").getAsJsonObject("minecraft:gameplay/natural_mob_spawns"))
                    .toString();
            for (String mob : List.of("nerospace:alien_villager", "nerospace:xertz_stalker",
                    "nerospace:quartz_crawler", "nerospace:greenling")) {
                assertTrue(spawners.contains(mob), b + " spawns " + mob);
            }
        }
    }

    @Test
    void surfaceIsBuiltFromGreenxertzBlocks() throws IOException {
        String noise = load("/data/nerospace/worldgen/noise_settings/greenxertz.json").toString();
        assertTrue(noise.contains("nerospace:xertz_soil"));
        assertTrue(noise.contains("nerospace:greenxertz_fungal_depths"));
        assertTrue(noise.contains("nerospace:greenxertz_spore_marsh"));
    }
}
