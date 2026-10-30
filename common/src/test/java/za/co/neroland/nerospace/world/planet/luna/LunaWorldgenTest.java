package za.co.neroland.nerospace.world.planet.luna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/** Luna's data wiring: dimension heights, surface rules, biome feature lists, cheese placement, loot in both trees. */
class LunaWorldgenTest {

    private static final List<String> BIOMES = List.of("luna_highlands", "luna_maria", "luna_crater_field");
    private static final String CHEESE = "nerospace:luna_cheese_ore";

    @BeforeEach
    void needsTree() {
        Assumptions.assumeTrue(LunaFiles.commonMain() != null, "common/src/main not found");
    }

    @Test
    @DisplayName("dimension_type luna: -64 / 384 / 384, scale 1.0, fixed time, airless sky type")
    void dimensionType() throws IOException {
        JsonObject t = LunaFiles.json(LunaFiles.data("resources", "dimension_type/luna.json"));
        assertEquals(-64, t.get("min_y").getAsInt());
        assertEquals(384, t.get("height").getAsInt());
        assertEquals(384, t.get("logical_height").getAsInt());
        assertEquals(1.0, t.get("coordinate_scale").getAsDouble());
        assertTrue(t.get("has_fixed_time").getAsBoolean());
        JsonObject d = LunaFiles.json(LunaFiles.data("resources", "dimension/luna.json"));
        assertEquals("nerospace:luna", d.get("type").getAsString());
        assertEquals("nerospace:luna", d.getAsJsonObject("generator").get("settings").getAsString());
        Set<String> biomes = new TreeSet<>();
        for (JsonElement e : d.getAsJsonObject("generator").getAsJsonObject("biome_source").getAsJsonArray("biomes")) {
            biomes.add(e.getAsJsonObject().get("biome").getAsString());
        }
        assertEquals(new TreeSet<>(BIOMES.stream().map(b -> "nerospace:" + b).toList()), biomes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("surface rules place only Luna blocks (plus the bedrock floor), no fluid")
    void surfaceRules(String tree) throws IOException {
        JsonObject ns = LunaFiles.json(LunaFiles.data(tree, "worldgen/noise_settings/luna.json"));
        JsonElement rule = ns.has("material_rule") ? ns.get("material_rule") : ns.get("surface_rule");
        Set<String> placed = new TreeSet<>();
        Matcher m = Pattern.compile("\"(?:Name|result_state)\":\"([a-z0-9_:]+)\"").matcher(rule.toString());
        while (m.find()) {
            placed.add(m.group(1));
        }
        assertEquals(Set.of("minecraft:bedrock", "nerospace:anorthosite", "nerospace:lunar_basalt", "nerospace:regolith"),
                placed, tree);
        assertTrue(ns.get("default_block").toString().contains("nerospace:lunar_basalt"), tree);
        assertTrue(ns.get("default_fluid").toString().contains("minecraft:air"), tree);
    }

    @Test
    @DisplayName("cheese: only luna_crater_field lists it; 1-in-4 chunks, size 4, just under the surface")
    void cheese() throws IOException {
        for (String b : BIOMES) {
            String features = LunaFiles.json(LunaFiles.data("resources", "worldgen/biome/" + b + ".json"))
                    .get("features").toString();
            assertEquals(b.equals("luna_crater_field"), features.contains(CHEESE), b);
        }
        Path biomes = LunaFiles.data("resources", "worldgen/biome");
        try (var files = Files.list(biomes)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                if (!f.getFileName().toString().equals("luna_crater_field.json")) {
                    assertFalse(Files.readString(f, StandardCharsets.UTF_8).contains(CHEESE), f.toString());
                }
            }
        }
        String placed = LunaFiles.json(LunaFiles.data("resources", "worldgen/placed_feature/luna_cheese_ore.json")).toString();
        assertTrue(placed.contains("\"type\":\"minecraft:rarity_filter\",\"chance\":4"), placed);
        // Surface-relative, so the blobs land in the regolith / anorthosite skin rather than the basalt below it.
        for (String tree : new String[] {"resources", "resources-26.3"}) {
            String p = LunaFiles.json(LunaFiles.data(tree, "worldgen/placed_feature/luna_cheese_ore.json")).toString();
            assertTrue(p.contains("\"type\":\"minecraft:rarity_filter\",\"chance\":4"), tree + " " + p);
            assertTrue(p.contains("WORLD_SURFACE_WG"), tree + " " + p);
            assertTrue(p.contains("resources".equals(tree) ? "\"y_spread\":-3" : "\"type\":\"minecraft:offset\""), p);
            assertFalse(p.contains("height_range") || p.contains("resources".equals(tree) ? "minecraft:offset" : "random_offset"), p);
        }
        assertEquals(4, LunaFiles.json(LunaFiles.data("resources", "worldgen/configured_feature/luna_cheese_ore.json"))
                .getAsJsonObject("config").get("size").getAsInt());
        assertEquals(4, LunaFiles.json(LunaFiles.data("resources-26.3", "worldgen/feature/luna_cheese_ore.json"))
                .get("size").getAsInt());
    }

    @Test
    @DisplayName("He-3 pockets only in the maria")
    void helium3() throws IOException {
        for (String b : BIOMES) {
            String features = LunaFiles.json(LunaFiles.data("resources", "worldgen/biome/" + b + ".json"))
                    .get("features").toString();
            assertEquals(b.equals("luna_maria"), features.contains("nerospace:luna_helium3_pocket"), b);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"regolith", "lunar_basalt", "polished_lunar_basalt", "anorthosite", "crater_glass",
            "ilmenite_ore", "helium3_pocket", "cheese_ore", "cheese_wheel"})
    @DisplayName("every Luna block has a loot table in both resource trees")
    void loot(String id) throws IOException {
        for (String tree : List.of("resources", "resources-26.3")) {
            JsonObject t = LunaFiles.json(LunaFiles.data(tree, "loot_table/blocks/" + id + ".json"));
            assertEquals("minecraft:block", t.get("type").getAsString(), tree + " " + id);
        }
    }

    @Test
    @DisplayName("every Luna placed feature has its configured feature in both trees")
    void features() throws IOException {
        try (var files = Files.list(LunaFiles.data("resources", "worldgen/placed_feature"))) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.getFileName().toString().startsWith("luna_"))::iterator) {
                String id = f.getFileName().toString();
                assertTrue(Files.isRegularFile(LunaFiles.data("resources", "worldgen/configured_feature/" + id)), id);
                assertTrue(Files.isRegularFile(LunaFiles.data("resources-26.3", "worldgen/feature/" + id)), id);
            }
        }
    }

    @Test
    @DisplayName("cheese keeps its camouflage (regolith / anorthosite only); crater glass sits in the crater surface")
    void cheeseTargetsAndCraterGlass() throws IOException {
        for (String tree : new String[] {"resources", "resources-26.3"}) {
            String dir = "resources".equals(tree) ? "worldgen/configured_feature/" : "worldgen/feature/";
            String cheese = LunaFiles.json(LunaFiles.data(tree, dir + "luna_cheese_ore.json")).toString();
            assertTrue(cheese.contains("nerospace:regolith") && cheese.contains("nerospace:anorthosite"), tree);
            assertFalse(cheese.contains("nerospace:lunar_basalt"), tree + ": no cheese in basalt");
            String glass = LunaFiles.json(LunaFiles.data(tree, dir + "luna_crater_glass.json")).toString();
            assertTrue(glass.contains("nerospace:lunar_basalt") && glass.contains("nerospace:regolith")
                    && glass.contains("nerospace:anorthosite"), tree);
            String placed = LunaFiles.json(LunaFiles.data(tree, "worldgen/placed_feature/luna_crater_glass.json")).toString();
            assertTrue(placed.contains("\"type\":\"minecraft:count\",\"count\":2"), placed);
            assertTrue(placed.contains("WORLD_SURFACE_WG"), placed);
            assertTrue(placed.contains("resources".equals(tree) ? "\"y_spread\":-3" : "\"y\":-3"), placed);
            assertFalse(placed.contains("rarity_filter") || placed.contains("height_range"), placed);
        }
    }
}
