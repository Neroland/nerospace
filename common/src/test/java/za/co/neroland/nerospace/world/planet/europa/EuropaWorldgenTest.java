package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Europa's dimension, noise settings, surface rules and biome files, read as JSON (no game bootstrap). */
class EuropaWorldgenTest {

    private static final List<String> KEYS = List.of("temperature", "humidity", "continentalness", "erosion",
            "weirdness", "depth", "offset");
    /** The isolation tests' pattern: no Europa file may match it. */
    private static final Pattern BANNED = Pattern.compile("(?<!cave_|sloped_)che" + "ese");

    private static JsonArray entries() throws IOException {
        JsonObject dim = EuropaFiles.json(EuropaFiles.data("resources", "dimension/europa.json"));
        assertEquals("nerospace:europa", dim.get("type").getAsString());
        JsonObject gen = dim.getAsJsonObject("generator");
        assertEquals("minecraft:noise", gen.get("type").getAsString());
        assertEquals("nerospace:europa", gen.get("settings").getAsString());
        assertEquals("minecraft:multi_noise", gen.getAsJsonObject("biome_source").get("type").getAsString());
        return gen.getAsJsonObject("biome_source").getAsJsonArray("biomes");
    }

    private static double[] range(JsonObject parameters, String key) {
        JsonArray a = parameters.getAsJsonArray(key);
        return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble()};
    }

    /** The biome a point resolves to (the first entry whose intervals all contain it; the table does not overlap). */
    private static String biomeAt(double depth, double continentalness, double erosion, double weirdness)
            throws IOException {
        for (JsonElement e : entries()) {
            JsonObject p = e.getAsJsonObject().getAsJsonObject("parameters");
            if (in(range(p, "depth"), depth) && in(range(p, "continentalness"), continentalness)
                    && in(range(p, "erosion"), erosion) && in(range(p, "weirdness"), weirdness)) {
                return e.getAsJsonObject().get("biome").getAsString();
            }
        }
        return "";
    }

    private static boolean in(double[] r, double v) {
        return v >= r[0] && v <= r[1];
    }

    @Test
    @DisplayName("dimension: the seven natural biomes in 11 entries, every key present, depth bands tile [-1, 2]")
    void dimension() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        JsonArray entries = entries();
        assertEquals(11, entries.size());
        Set<String> biomes = new TreeSet<>();
        Set<Double> edges = new TreeSet<>();
        for (JsonElement e : entries) {
            JsonObject p = e.getAsJsonObject().getAsJsonObject("parameters");
            for (String k : KEYS) {
                assertTrue(p.has(k), e + " lacks " + k);
            }
            assertEquals(0.0, p.get("offset").getAsDouble());
            assertEquals(-1.0, range(p, "temperature")[0]);
            assertEquals(1.0, range(p, "humidity")[1]);
            biomes.add(e.getAsJsonObject().get("biome").getAsString().substring("nerospace:".length()));
            edges.add(range(p, "depth")[0]);
            edges.add(range(p, "depth")[1]);
        }
        assertEquals(new TreeSet<>(EuropaFiles.NATURAL_BIOMES), biomes);
        // No gap: the band edges are exactly the four layer boundaries plus the two ends.
        assertEquals(new TreeSet<>(List.of(-1.0, 0.1875, 0.875, 1.5625, 2.0)), edges);
        assertEquals(0.1875, EuropaNumbers.depthAt(EuropaNumbers.BIOME_SURFACE_Y), 1.0E-9);
        assertEquals(0.8125, EuropaNumbers.depthAt(EuropaNumbers.SEA_LEVEL), 1.0E-9);
        assertEquals(0.875, EuropaNumbers.depthAt(EuropaNumbers.BIOME_OCEAN_Y), 1.0E-9);
        assertEquals(1.5625, EuropaNumbers.depthAt(EuropaNumbers.BIOME_VENT_Y), 1.0E-9);
        assertEquals(2.0, EuropaNumbers.depthAt(EuropaNumbers.MIN_Y), 1.0E-9);
        // The surface biomes start 8 blocks under the lowest possible surface.
        assertEquals(8, EuropaNumbers.SURFACE_MIN - EuropaNumbers.BIOME_SURFACE_Y);
    }

    @Test
    @DisplayName("biome lookup by layer: surface set on top, shell at sea level, abyss below, vent fields on the floor")
    void layersResolve() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        double surface = EuropaNumbers.depthAt(200);
        assertEquals("nerospace:europa_lineae", biomeAt(surface, 0.0, 0.0, 0.0));
        assertEquals("nerospace:europa_chaos", biomeAt(surface, 0.0, 0.6, 0.5));
        assertEquals("nerospace:europa_chaos", biomeAt(surface, 0.0, 0.6, -0.5));
        assertEquals("nerospace:europa_penitentes", biomeAt(surface, 0.0, -0.7, 0.5));
        assertEquals("nerospace:europa_ice_plains", biomeAt(surface, 0.0, 0.0, -0.5));
        // Sea level sits inside the shell biome, 8 blocks from its lower edge (habitats generate there later).
        assertEquals("nerospace:europa_shell", biomeAt(EuropaNumbers.depthAt(EuropaNumbers.SEA_LEVEL), 0.0, 0.0, 0.0));
        assertEquals(8, EuropaNumbers.SEA_LEVEL - EuropaNumbers.BIOME_OCEAN_Y);
        assertEquals("nerospace:europa_abyss", biomeAt(EuropaNumbers.depthAt(40), 0.9, 0.0, 0.0));
        assertEquals("nerospace:europa_abyss", biomeAt(EuropaNumbers.depthAt(-30), -0.5, 0.0, 0.0));
        assertEquals("nerospace:europa_vent_fields", biomeAt(EuropaNumbers.depthAt(-30), 0.5, 0.0, 0.0));
        // Vent-field water reaches at least 12 blocks above the highest possible floor.
        assertTrue(EuropaNumbers.BIOME_VENT_Y - EuropaNumbers.FLOOR_MAX >= 12);
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("noise settings: sea level 88, shell ice and water, no aquifers, the router on Europa's own functions")
    void noiseSettings(String tree) throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        JsonObject s = EuropaFiles.json(EuropaFiles.data(tree, "worldgen/noise_settings/europa.json"));
        assertEquals(EuropaNumbers.SEA_LEVEL, s.get("sea_level").getAsInt());
        assertFalse(s.get("disable_mob_generation").getAsBoolean());
        assertFalse(s.get("legacy_random_source").getAsBoolean());
        assertEquals(0, s.getAsJsonArray("spawn_target").size());
        assertEquals(EuropaNumbers.MIN_Y, s.getAsJsonObject("noise").get("min_y").getAsInt());
        assertEquals(EuropaNumbers.HEIGHT, s.getAsJsonObject("noise").get("height").getAsInt());
        JsonObject router = s.getAsJsonObject("noise_router");
        for (String k : List.of("continents", "erosion", "ridges", "temperature", "vegetation", "depth",
                "final_density")) {
            assertEquals("nerospace:europa/" + k, router.get(k).getAsString(), tree + " " + k);
        }
        if (tree.equals("resources")) {
            assertFalse(s.get("aquifers_enabled").getAsBoolean());
            assertFalse(s.get("ore_veins_enabled").getAsBoolean());
            assertEquals("nerospace:shell_ice", s.getAsJsonObject("default_block").get("Name").getAsString());
            assertEquals("minecraft:water", s.getAsJsonObject("default_fluid").get("Name").getAsString());
            assertEquals(15, router.size());
            for (String k : List.of("barrier", "fluid_level_floodedness", "fluid_level_spread", "lava", "vein_toggle",
                    "vein_ridged", "vein_gap")) {
                assertEquals(0.0, router.get(k).getAsDouble(), k);
            }
            // The shape the 1.21.x rewrite recognises: add(-0.390625, add(0.390625, topside)), inline.
            JsonObject level = router.getAsJsonObject("preliminary_surface_level");
            assertEquals("minecraft:find_top_surface", level.get("type").getAsString());
            JsonObject density = level.getAsJsonObject("density");
            assertEquals(-0.390625, density.get("argument1").getAsDouble());
            assertEquals(0.390625, density.getAsJsonObject("argument2").get("argument1").getAsDouble());
            assertEquals("nerospace:europa/topside", density.getAsJsonObject("argument2").get("argument2").getAsString());
            assertTrue(s.has("surface_rule") && !s.has("material_rule"));
        } else {
            assertFalse(s.has("aquifers"), "no aquifers object: open cells below sea level fill with the default fluid");
            assertFalse(s.has("aquifers_enabled"));
            assertEquals("nerospace:shell_ice", s.get("default_block").getAsString());
            assertEquals("minecraft:water", s.get("default_fluid").getAsString());
            assertEquals(Set.of("chunk_surface_level", "continents", "depth", "erosion", "final_density", "ridges",
                    "temperature", "vegetation"), router.keySet());
            assertEquals("nerospace:europa/chunk_surface_level", router.get("chunk_surface_level").getAsString());
            assertTrue(Files.isRegularFile(EuropaFiles.data(tree,
                    "worldgen/density_function/europa/chunk_surface_level.json")));
            assertEquals(6, s.getAsJsonArray("debug_functions").size());
            assertTrue(s.has("material_rule") && !s.has("surface_rule"));
        }
    }

    private static void results(JsonElement node, Set<String> blocks, Set<String> types) {
        if (node.isJsonArray()) {
            node.getAsJsonArray().forEach(e -> results(e, blocks, types));
            return;
        }
        if (!node.isJsonObject()) {
            return;
        }
        JsonObject o = node.getAsJsonObject();
        if (o.has("type")) {
            types.add(o.get("type").getAsString());
        }
        if (o.has("result_state")) {
            JsonElement state = o.get("result_state");
            blocks.add(state.isJsonObject() ? state.getAsJsonObject().get("Name").getAsString() : state.getAsString());
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            results(e.getValue(), blocks, types);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("surface rules write only bedrock, vent basalt and the three surface blocks, by plain Y and biome tests")
    void surfaceRules(String tree) throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        JsonObject s = EuropaFiles.json(EuropaFiles.data(tree, "worldgen/noise_settings/europa.json"));
        JsonElement rule = s.get(tree.equals("resources") ? "surface_rule" : "material_rule");
        Set<String> blocks = new TreeSet<>();
        Set<String> types = new TreeSet<>();
        results(rule, blocks, types);
        assertEquals(new TreeSet<>(List.of("minecraft:bedrock", "nerospace:vent_basalt", "nerospace:lineae_salt",
                "nerospace:chaos_ice", "nerospace:europa_ice")), blocks);
        assertFalse(types.contains("minecraft:above_preliminary_surface"));
        assertEquals(new TreeSet<>(List.of("minecraft:sequence", "minecraft:condition", "minecraft:block",
                "minecraft:vertical_gradient", "minecraft:y_above", "minecraft:biome", "minecraft:stone_depth")), types);
        // The floor rule is a one-block-wide gradient (a plain Y test) well under the ocean and above every floor.
        assertTrue(rule.toString().contains("\"true_at_and_below\":{\"absolute\":-5}"));
        assertTrue(rule.toString().contains("\"false_at_and_above\":{\"absolute\":-4}"));
        assertTrue(-5 > EuropaNumbers.FLOOR_MAX);
        assertTrue(rule.toString().contains("\"anchor\":{\"absolute\":" + EuropaNumbers.BIOME_SURFACE_Y + "}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("biomes: ten files, no carvers, no precipitation, temperatures that freeze or keep the sea open")
    void biomes(String tree) throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        List<String> all = new ArrayList<>(EuropaFiles.NATURAL_BIOMES);
        all.addAll(EuropaFiles.TRIO);
        assertEquals(10, all.size());
        for (String b : all) {
            JsonObject j = EuropaFiles.json(EuropaFiles.data(tree, "worldgen/biome/" + b + ".json"));
            assertEquals(0, j.getAsJsonArray("carvers").size(), tree + " " + b);
            assertFalse(j.get("has_precipitation").getAsBoolean(), tree + " " + b);
            boolean open = b.equals("terraformed_europa_hydrated") || b.equals("terraformed_europa_living");
            assertEquals(open ? 0.2F : 0.0F, j.get("temperature").getAsFloat(), tree + " " + b);
        }
        Set<String> found = new HashSet<>();
        try (Stream<Path> files = Files.list(EuropaFiles.data(tree, "worldgen/biome"))) {
            files.map(f -> f.getFileName().toString()).filter(n -> n.contains("europa"))
                    .forEach(n -> found.add(n.replace(".json", "")));
        }
        assertEquals(new HashSet<>(all), found, tree);
    }

    @Test
    @DisplayName("biome tags: the surface is cold, irradiated and tagged as surface; everything under it is mild")
    void biomeTags() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        for (String tag : List.of("hazard/cold", "hazard/radiation", "europa_surface")) {
            String values = EuropaFiles.json(EuropaFiles.data("resources", "tags/worldgen/biome/" + tag + ".json"))
                    .getAsJsonArray("values").toString();
            for (String b : EuropaFiles.SURFACE_BIOMES) {
                assertTrue(values.contains("\"nerospace:" + b + "\""), tag + " " + b);
            }
            for (String b : List.of("europa_shell", "europa_abyss", "europa_vent_fields", "terraformed_europa_living")) {
                assertFalse(values.contains("\"nerospace:" + b + "\""), tag + " " + b);
            }
        }
        String mild = EuropaFiles.json(EuropaFiles.data("resources", "tags/worldgen/biome/hazard/cold_mild.json"))
                .getAsJsonArray("values").toString();
        for (String b : List.of("europa_shell", "europa_abyss", "europa_vent_fields", "terraformed_europa_rooted",
                "terraformed_europa_hydrated", "terraformed_europa_living")) {
            assertTrue(mild.contains("\"nerospace:" + b + "\""), b);
        }
        for (String b : EuropaFiles.SURFACE_BIOMES) {
            assertFalse(mild.contains("\"nerospace:" + b + "\""), b);
        }
    }

    @Test
    @DisplayName("no Europa worldgen file contains the banned word")
    void bannedWord() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        int scanned = 0;
        for (String tree : EuropaFiles.TREES) {
            try (Stream<Path> files = Files.walk(EuropaFiles.data(tree, "worldgen"))) {
                for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().replace('\\', '/').contains("europa"))::iterator) {
                    scanned++;
                    assertFalse(BANNED.matcher(Files.readString(f, StandardCharsets.UTF_8)).find(), f.toString());
                    assertFalse(BANNED.matcher(f.getFileName().toString()).find(), f.toString());
                }
            }
        }
        assertTrue(scanned >= 60, "scanned " + scanned);
    }
}
