package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Nyxara crystal blocks: assets and loot in both resource trees, the translucent textures really carry alpha (the
 * chunk layer comes from texture alpha on every 26.x line), the 26.3 worldgen overlays use the flattened state-provider
 * form, and the surface rules put crystal on the floor of every band.
 */
class NyxaraCrystalDataTest {

    static final List<String> BLOCKS = List.of("sunglass_crystal", "crazed_crystal", "shard_drift", "twilight_crystal",
            "rose_crystal", "crystal_spire", "nyx_prism", "budding_nyx_crystal", "nyx_crystal_cluster",
            "smoke_crystal", "mirror_crystal");

    /** Every texture of the three translucent blocks (spire, prism, mirror lake). */
    static final List<String> TRANSLUCENT = List.of("crystal_spire", "crystal_spire_top", "nyx_prism",
            "nyx_prism_flaring", "mirror_crystal");

    private static Path main() {
        Path main = PlanetNoiseTest.main();
        Assumptions.assumeTrue(main != null, "common sources not found");
        return main;
    }

    private static JsonObject json(Path f) throws IOException {
        assertTrue(Files.isRegularFile(f), "missing " + f);
        return JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    @DisplayName("every crystal block has a blockstate, a client item, and loot in both trees")
    void assetsAndLoot() throws IOException {
        Path m = main();
        for (String id : BLOCKS) {
            assertTrue(Files.isRegularFile(m.resolve("resources/assets/nerospace/blockstates/" + id + ".json")), id);
            assertTrue(Files.isRegularFile(m.resolve("resources/assets/nerospace/items/" + id + ".json")), id);
            for (String tree : List.of("resources", "resources-26.3")) {
                JsonObject loot = json(m.resolve(tree + "/data/nerospace/loot_table/blocks/" + id + ".json"));
                assertEquals("minecraft:block", loot.get("type").getAsString(), tree + " " + id);
            }
            String legacy = Files.readString(m.resolve("resources/data/nerospace/loot_table/blocks/" + id + ".json"));
            String modern = Files.readString(
                    m.resolve("resources-26.3/data/nerospace/loot_table/blocks/" + id + ".json"));
            assertFalse(legacy.contains("\"modifier\""), id + ": legacy tree uses functions");
            assertFalse(modern.contains("\"functions\""), id + ": 26.3 tree uses modifier");
            // 26.3 reads a bare string condition as a predicate-file reference; only vanilla's tool/* files exist.
            assertFalse(modern.replaceAll("\\s", "").contains("\"condition\":\"minecraft:survives_explosion\""), id);
            // Loot number providers take min / max on every line (min_inclusive is the int-provider form).
            assertFalse(modern.contains("min_inclusive") || legacy.contains("min_inclusive"), id);
        }
        assertTrue(Files.isRegularFile(m.resolve("resources/assets/nerospace/textures/item/crystal_shard.png")));
    }

    @Test
    @DisplayName("every translucent Nyxara crystal texture has real partial alpha (1-254)")
    void translucentTexturesHaveAlpha() throws IOException {
        Path tex = main().resolve("resources/assets/nerospace/textures/block");
        for (String id : TRANSLUCENT) {
            BufferedImage img = ImageIO.read(tex.resolve(id + ".png").toFile());
            int partial = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int a = img.getRGB(x, y) >>> 24;
                    partial += a > 0 && a < 255 ? 1 : 0;
                }
            }
            assertTrue(partial > 0, id + " has no partial alpha, so it would render SOLID");
        }
    }

    @Test
    @DisplayName("26.3 geode and lake overlays use object state providers and hashed tags")
    void modernProviders() throws IOException {
        Path f = main().resolve("resources-26.3/data/nerospace/worldgen/feature");
        JsonObject geode = json(f.resolve("nyxara_crystal_geode.json")).getAsJsonObject("blocks");
        for (String k : List.of("filling_provider", "inner_layer_provider", "alternate_inner_layer_provider",
                "middle_layer_provider", "outer_layer_provider")) {
            assertTrue(geode.get(k).isJsonObject(), k + " must be {id, properties}, not a provider reference");
        }
        assertTrue(geode.get("cannot_replace").getAsString().startsWith("#"));
        assertTrue(geode.get("invalid_blocks").getAsString().startsWith("#"));
        JsonObject lake = json(f.resolve("nyxara_mirror_lake.json"));
        for (String k : List.of("fluid", "barrier", "can_place_feature", "can_replace_with_air_or_fluid",
                "can_replace_with_barrier")) {
            assertTrue(lake.has(k) && lake.get(k).isJsonObject(), "lake " + k);
        }
        JsonObject legacyLake = json(main().resolve(
                "resources/data/nerospace/worldgen/configured_feature/nyxara_mirror_lake.json")).getAsJsonObject("config");
        assertTrue(legacyLake.has("can_replace_with_barrier"), "26.2 lake needs the predicates too");
        // Int providers are flat ({type, min_inclusive, max_inclusive}) on every line; the nested "value" form is gone.
        for (String id : List.of("nyxara_crystal_spire", "nyxara_prism_spire")) {
            String spire = Files.readString(main().resolve(
                    "resources/data/nerospace/worldgen/configured_feature/" + id + ".json"), StandardCharsets.UTF_8);
            assertFalse(spire.contains("\"value\""), id + " uses the nested uniform form");
        }
    }

    @Test
    @DisplayName("each band's surface rule floors with its crystal; the vanilla stand-ins are gone")
    void surfaceIsCrystal() throws IOException {
        for (String tree : List.of("resources", "resources-26.3")) {
            String s = Files.readString(main().resolve(tree + "/data/nerospace/worldgen/noise_settings/nyxara.json"),
                    StandardCharsets.UTF_8);
            for (String block : List.of("sunglass_crystal", "twilight_crystal", "smoke_crystal")) {
                assertTrue(s.contains("nerospace:" + block), tree + " " + block);
            }
            assertFalse(s.contains("minecraft:smooth_basalt"), tree);
            assertFalse(s.contains("minecraft:blackstone"), tree);
        }
        List<String> missing = new ArrayList<>();
        for (String b : List.of("nyxara_dayside", "nyxara_twilight_band", "nyxara_nightside")) {
            JsonObject biome = json(main().resolve("resources/data/nerospace/worldgen/biome/" + b + ".json"));
            for (JsonElement step : biome.getAsJsonArray("features")) {
                for (JsonElement e : step.getAsJsonArray()) {
                    String id = e.getAsString().substring("nerospace:".length());
                    if (!Files.isRegularFile(main().resolve(
                            "resources/data/nerospace/worldgen/placed_feature/" + id + ".json"))) {
                        missing.add(b + " -> " + id);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), missing.toString());
    }

    @Test
    @DisplayName("the crystal cluster has no tool requirement, so its hand drop is reachable")
    void clusterDropsByHand() throws IOException {
        String src = Files.readString(main().resolve(
                "java/za/co/neroland/nerospace/world/planet/nyxara/NyxaraContent.java"), StandardCharsets.UTF_8);
        int start = src.indexOf("\"nyx_crystal_cluster\"");
        assertTrue(start > 0, "cluster registration not found");
        int end = src.indexOf(";", start);
        assertFalse(src.substring(start, end).contains("requiresCorrectToolForDrops"),
                "nyx_crystal_cluster must not require the correct tool");
    }

    @Test
    @DisplayName("Crystal Shard carries the Core materials highlight, like Void Shard")
    void shardIsHighlighted() throws IOException {
        String tag = Files.readString(main().resolve("resources/data/neroland/tags/item/highlight/materials.json"),
                StandardCharsets.UTF_8);
        assertTrue(tag.contains("\"nerospace:crystal_shard\""));
        assertTrue(tag.contains("\"nerospace:void_shard\""));
    }

    @Test
    @DisplayName("mirror lakes are tried in one nightside chunk in three (the lake feature rejects uneven rims)")
    void mirrorLakeRarity() throws IOException {
        JsonObject placed = json(main().resolve(
                "resources/data/nerospace/worldgen/placed_feature/nyxara_mirror_lake_placed.json"));
        JsonObject rarity = placed.getAsJsonArray("placement").get(0).getAsJsonObject();
        assertEquals("minecraft:rarity_filter", rarity.get("type").getAsString());
        assertEquals(3, rarity.get("chance").getAsInt());
    }

    @Test
    @DisplayName("the band places Rose Crystal before the Void Crystal outcrops, so outcrops can replace rose")
    void roseBeforeVoidCrystal() throws IOException {
        for (String tree : List.of("resources", "resources-26.3")) {
            JsonObject biome = json(main().resolve(tree + "/data/nerospace/worldgen/biome/nyxara_twilight_band.json"));
            List<String> order = new ArrayList<>();
            for (JsonElement step : biome.getAsJsonArray("features")) {
                for (JsonElement e : step.getAsJsonArray()) {
                    order.add(e.getAsString());
                }
            }
            int rose = order.indexOf("nerospace:nyxara_rose_field_placed");
            int voidCrystal = order.indexOf("nerospace:nyxara_void_crystal_placed");
            assertTrue(rose >= 0 && voidCrystal >= 0 && rose < voidCrystal, tree + " " + order);
        }
    }
}
