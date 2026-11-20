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

    // --- Features (WP3) ---------------------------------------------------------------------------------------------

    /** The 13 configured features, present in both trees (`configured_feature` in R, `feature` in R263). */
    static final List<String> CONFIGURED = List.of("europa_brine_pocket", "europa_ice_fracture", "europa_oxidant_ice",
            "europa_brine_salt_ore_lineae", "europa_deuterium_ice", "europa_brine_salt_ore", "europa_vent_sulfide_ore",
            "europa_europium_ore", "europa_cryovent", "europa_penitente_patch", "europa_hydrothermal_vent",
            "europa_smoker_chimney", "europa_tube_mat_patch");
    /** The 14 placed features (the habitat places the existing hamlet). */
    static final List<String> PLACED = List.of("europa_brine_pocket_placed", "europa_ice_fracture_placed",
            "europa_habitat_placed", "europa_oxidant_ice_placed", "europa_brine_salt_ore_lineae_placed",
            "europa_deuterium_ice_placed", "europa_brine_salt_ore_placed", "europa_vent_sulfide_ore_placed",
            "europa_europium_ore_placed", "europa_cryovent_placed", "europa_penitente_patch_placed",
            "europa_hydrothermal_vent_placed", "europa_smoker_chimney_placed", "europa_tube_mat_patch_placed");
    /** The placed features that need a 26.3 copy (a modifier is spelled differently there). */
    static final Set<String> PLACED_263 = Set.of("europa_oxidant_ice_placed", "europa_brine_salt_ore_lineae_placed",
            "europa_cryovent_placed", "europa_penitente_patch_placed", "europa_smoker_chimney_placed",
            "europa_tube_mat_patch_placed");

    private static String configuredDir(String tree) {
        return tree.equals("resources") ? "worldgen/configured_feature/" : "worldgen/feature/";
    }

    /** The placed feature a tree loads: its own copy if it has one, else the shared file. */
    private static Path placed(String tree, String id) {
        Path own = EuropaFiles.data(tree, "worldgen/placed_feature/" + id + ".json");
        return Files.isRegularFile(own) ? own : EuropaFiles.data("resources", "worldgen/placed_feature/" + id + ".json");
    }

    private static JsonArray placement(String tree, String id) throws IOException {
        return EuropaFiles.json(placed(tree, id)).getAsJsonArray("placement");
    }

    private static List<String> features(String tree, String biome) throws IOException {
        List<String> out = new ArrayList<>();
        for (JsonElement step : EuropaFiles.json(EuropaFiles.data(tree, "worldgen/biome/" + biome + ".json"))
                .getAsJsonArray("features")) {
            step.getAsJsonArray().forEach(e -> out.add(e.getAsString()));
        }
        return out;
    }

    private static JsonObject modifier(JsonArray placement, String type) {
        for (JsonElement e : placement) {
            if (e.getAsJsonObject().get("type").getAsString().equals(type)) {
                return e.getAsJsonObject();
            }
        }
        throw new AssertionError("no " + type + " in " + placement);
    }

    private static int[] uniformRange(JsonArray placement) {
        JsonObject h = modifier(placement, "minecraft:height_range").getAsJsonObject("height");
        assertEquals("minecraft:uniform", h.get("type").getAsString());
        return new int[] {h.getAsJsonObject("min_inclusive").get("absolute").getAsInt(),
                h.getAsJsonObject("max_inclusive").get("absolute").getAsInt()};
    }

    @Test
    @DisplayName("habitat: the depth at Y 88 resolves to europa_shell, which lists the habitat placed at constant Y 88")
    void habitatBiomeListsHabitat() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        double depth = EuropaNumbers.depthAt(EuropaNumbers.SEA_LEVEL);
        for (double c : new double[] {-1.0, 0.0, 1.0}) {
            for (double e : new double[] {-1.0, 0.0, 1.0}) {
                assertEquals("nerospace:europa_shell", biomeAt(depth, c, e, 0.0));
            }
        }
        for (String tree : EuropaFiles.TREES) {
            assertTrue(features(tree, "europa_shell").contains("nerospace:europa_habitat_placed"), tree);
            JsonObject p = EuropaFiles.json(placed(tree, "europa_habitat_placed"));
            assertEquals("nerospace:hamlet", p.get("feature").getAsString());
            JsonObject h = modifier(p.getAsJsonArray("placement"), "minecraft:height_range").getAsJsonObject("height");
            assertEquals("minecraft:constant", h.get("type").getAsString());
            assertEquals(EuropaNumbers.SEA_LEVEL, h.getAsJsonObject("value").get("absolute").getAsInt());
            for (String b : EuropaFiles.NATURAL_BIOMES) {
                if (!b.equals("europa_shell")) {
                    assertFalse(features(tree, b).contains("nerospace:europa_habitat_placed"), tree + " " + b);
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("every Europa placed feature ends with a biome filter")
    void everyPlacedFeatureHasBiomeFilter(String tree) throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        for (String id : PLACED) {
            JsonArray p = placement(tree, id);
            assertEquals("minecraft:biome", p.get(p.size() - 1).getAsJsonObject().get("type").getAsString(),
                    tree + " " + id);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("every placed feature a Europa biome lists exists, and so does the configured feature it places")
    void featuresResolve(String tree) throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        for (String id : CONFIGURED) {
            for (String t : EuropaFiles.TREES) {
                assertTrue(Files.isRegularFile(EuropaFiles.data(t, configuredDir(t) + id + ".json")), t + " " + id);
            }
        }
        Set<String> listed = new TreeSet<>();
        for (String b : EuropaFiles.NATURAL_BIOMES) {
            listed.addAll(features(tree, b));
        }
        assertEquals(new TreeSet<>(PLACED.stream().map(p -> "nerospace:" + p).toList()), listed, tree);
        for (String id : listed) {
            Path file = placed(tree, id.substring("nerospace:".length()));
            assertTrue(Files.isRegularFile(file), tree + " " + id);
            String feature = EuropaFiles.json(file).get("feature").getAsString();
            assertTrue(Files.isRegularFile(EuropaFiles.data(tree,
                    configuredDir(tree) + feature.substring("nerospace:".length()) + ".json")), tree + " " + feature);
        }
        assertEquals(List.of(), features(tree, "europa_abyss"));
    }

    @Test
    @DisplayName("trees agree: a placed feature without a 26.3 copy uses no random_offset; each copy matches its base")
    void treesAgree() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        for (String id : PLACED) {
            Path own = EuropaFiles.data("resources-26.3", "worldgen/placed_feature/" + id + ".json");
            String base = Files.readString(placed("resources", id), StandardCharsets.UTF_8);
            assertEquals(PLACED_263.contains(id), Files.isRegularFile(own), id);
            if (!Files.isRegularFile(own)) {
                assertFalse(base.contains("minecraft:random_offset"), id);
                continue;
            }
            String copy = Files.readString(own, StandardCharsets.UTF_8);
            assertFalse(copy.contains("minecraft:random_offset"), id);
            JsonObject a = EuropaFiles.json(placed("resources", id));
            JsonObject b = EuropaFiles.json(own);
            assertEquals(a.get("feature"), b.get("feature"), id);
            JsonArray pa = a.getAsJsonArray("placement");
            JsonArray pb = b.getAsJsonArray("placement");
            assertEquals(pa.size(), pb.size(), id);
            for (int i = 0; i < pa.size(); i++) {
                String ta = pa.get(i).getAsJsonObject().get("type").getAsString();
                String tb = pb.get(i).getAsJsonObject().get("type").getAsString();
                assertEquals(ta.equals("minecraft:random_offset") ? "minecraft:offset" : ta, tb, id + " #" + i);
                if (!ta.equals("minecraft:random_offset")) {
                    assertEquals(pa.get(i), pb.get(i), id + " #" + i);
                }
            }
        }
        // No 1.21.x-incompatible spellings anywhere in the shared tree (the build rewrites none of these files).
        for (String id : PLACED) {
            String base = Files.readString(placed("resources", id), StandardCharsets.UTF_8);
            assertFalse(base.contains("minecraft:trapezoid"), id);
        }
        for (String id : CONFIGURED) {
            String text = Files.readString(EuropaFiles.data("resources", configuredDir("resources") + id + ".json"),
                    StandardCharsets.UTF_8);
            assertFalse(text.contains("minecraft:random_patch"), id);
        }
    }

    @Test
    @DisplayName("heights: shell ores inside the always-solid shell, europium inside the always-solid floor, vent scans land")
    void heights() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        for (String id : List.of("europa_brine_pocket_placed", "europa_deuterium_ice_placed",
                "europa_brine_salt_ore_placed")) {
            int[] r = uniformRange(placement("resources", id));
            assertTrue(r[0] >= EuropaNumbers.ROOF_MAX + 1 && r[1] <= EuropaNumbers.SURFACE_MIN - 1, id);
        }
        assertEquals(List.of(104, 160), List.of(uniformRange(placement("resources", "europa_brine_pocket_placed"))[0],
                uniformRange(placement("resources", "europa_brine_pocket_placed"))[1]));
        assertEquals(166, uniformRange(placement("resources", "europa_deuterium_ice_placed"))[1]);
        int[] eu = uniformRange(placement("resources", "europa_europium_ore_placed"));
        assertTrue(eu[0] >= EuropaNumbers.MIN_Y && eu[1] <= EuropaNumbers.FLOOR_MIN - 1, "europium band");
        assertEquals(List.of(-60, -46), List.of(eu[0], eu[1]));
        assertEquals(6, modifier(placement("resources", "europa_europium_ore_placed"), "minecraft:rarity_filter")
                .get("chance").getAsInt());
        assertEquals(16, modifier(placement("resources", "europa_ice_fracture_placed"), "minecraft:rarity_filter")
                .get("chance").getAsInt());
        assertEquals(24, modifier(placement("resources", "europa_cryovent_placed"), "minecraft:rarity_filter")
                .get("chance").getAsInt());
        int[] sulfide = uniformRange(placement("resources", "europa_vent_sulfide_ore_placed"));
        assertEquals(List.of(-60, -24), List.of(sulfide[0], sulfide[1]));
        for (String tree : EuropaFiles.TREES) {
            JsonObject ore = EuropaFiles.json(EuropaFiles.data(tree, configuredDir(tree) + "europa_europium_ore.json"));
            JsonObject cfg = ore.has("config") ? ore.getAsJsonObject("config") : ore;
            assertEquals(3, cfg.get("size").getAsInt(), tree);
            for (String b : EuropaFiles.NATURAL_BIOMES) {
                assertEquals(b.equals("europa_vent_fields"),
                        features(tree, b).contains("nerospace:europa_europium_ore_placed"), tree + " " + b);
            }
        }
        // The vent scans start in always-open water and reach below the lowest possible floor.
        for (String id : List.of("europa_hydrothermal_vent_placed", "europa_smoker_chimney_placed",
                "europa_tube_mat_patch_placed")) {
            JsonArray p = placement("resources", id);
            int[] start = uniformRange(p);
            assertTrue(start[0] > EuropaNumbers.FLOOR_MAX && start[1] < EuropaNumbers.ROOF_MIN, id);
            JsonObject scan = modifier(p, "minecraft:environment_scan");
            assertEquals("down", scan.get("direction_of_search").getAsString());
            assertTrue(start[0] - scan.get("max_steps").getAsInt() < EuropaNumbers.FLOOR_MIN, id);
            assertTrue(scan.toString().contains("nerospace:vent_basalt") && scan.toString().contains("minecraft:water"));
            assertTrue(EuropaNumbers.depthAt(start[1]) >= 1.5625, id + " starts inside the vent-field band");
        }
    }

    /** A block state as either tree writes it: name and properties. */
    private static String state(JsonElement s) {
        if (s.isJsonPrimitive()) {
            return s.getAsString();
        }
        JsonObject o = s.getAsJsonObject();
        if (o.has("type") && o.has("state")) {
            return state(o.get("state"));
        }
        String name = o.has("Name") ? o.get("Name").getAsString() : o.get("id").getAsString();
        JsonObject props = o.has("Properties") ? o.getAsJsonObject("Properties")
                : o.has("properties") ? o.getAsJsonObject("properties") : new JsonObject();
        return name + new TreeSet<>(props.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().getAsString())
                .toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("generated states: charged ore, active vents, capped waterlogged chimneys, waterlogged mats")
    void generatedStates(String tree) throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        java.util.function.Function<String, JsonObject> cfg = id -> {
            try {
                JsonObject j = EuropaFiles.json(EuropaFiles.data(tree, configuredDir(tree) + id + ".json"));
                return j.has("config") ? j.getAsJsonObject("config") : j;
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        };
        assertEquals("nerospace:oxidant_ice[charge=3]", state(cfg.apply("europa_oxidant_ice")
                .getAsJsonArray("targets").get(0).getAsJsonObject().get("state")));
        assertEquals("nerospace:hydrothermal_vent[active=true]",
                state(cfg.apply("europa_hydrothermal_vent").get("to_place")));
        assertEquals("nerospace:cryovent[charged=true]", state(cfg.apply("europa_cryovent").get("to_place")));
        assertEquals("nerospace:tube_mat[waterlogged=true]", state(cfg.apply("europa_tube_mat_patch").get("to_place")));
        JsonArray layers = cfg.apply("europa_smoker_chimney").getAsJsonArray("layers");
        assertEquals(2, layers.size());
        assertEquals("nerospace:smoker_chimney[capped=false, waterlogged=true]",
                state(layers.get(0).getAsJsonObject().get("provider")));
        assertEquals("nerospace:smoker_chimney[capped=true, waterlogged=true]",
                state(layers.get(1).getAsJsonObject().get("provider")));
        assertTrue(cfg.apply("europa_smoker_chimney").get("prioritize_tip").getAsBoolean(), "the capped tip is kept");
        // vent_basalt is never written by a feature (its natural DISTANCE stays the default 4).
        for (String id : CONFIGURED) {
            String text = Files.readString(EuropaFiles.data(tree, configuredDir(tree) + id + ".json"),
                    StandardCharsets.UTF_8);
            assertFalse(text.contains("\"distance\""), id);
        }
    }
}
