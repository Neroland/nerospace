package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Proves Europa's layers from the density-function JSON alone, by interval arithmetic: a noise is somewhere in
 * [-2, 2] (its clamp then bounds it), a Y ramp is exact, references are read from the files. For every Y the test
 * computes the lowest and highest value {@code final_density} can take and checks it against the layer table.
 */
class EuropaDensityTest {

    private static final String DF = "worldgen/density_function/europa/";
    private static final List<String> LAYERS = List.of("topside", "underside", "seabed");
    /** Floating-point slack: a bound that is exactly zero on paper may come out one rounding step either side. */
    private static final double EPS = 1.0E-9;

    private static JsonElement df(String tree, String name) throws IOException {
        return EuropaFiles.json(EuropaFiles.data(tree, DF + name + ".json"));
    }

    private static JsonElement arg(JsonObject o, String... names) {
        for (String n : names) {
            if (o.has(n)) {
                return o.get(n);
            }
        }
        throw new AssertionError("none of " + List.of(names) + " in " + o);
    }

    /** Lowest and highest value a node can take at {@code y}. */
    private static double[] eval(String tree, JsonElement node, int y) throws IOException {
        if (node.isJsonPrimitive() && node.getAsJsonPrimitive().isNumber()) {
            double v = node.getAsDouble();
            return new double[] {v, v};
        }
        if (node.isJsonPrimitive()) {
            String id = node.getAsString();
            assertTrue(id.startsWith("nerospace:europa/"), "foreign density function " + id);
            return eval(tree, df(tree, id.substring("nerospace:europa/".length())), y);
        }
        JsonObject o = node.getAsJsonObject();
        String type = o.get("type").getAsString();
        switch (type) {
            case "minecraft:noise":
                return new double[] {-2.0, 2.0};
            case "minecraft:y_clamped_gradient":
                return ramp(o.get("from_y").getAsInt(), o.get("from_value").getAsDouble(), o.get("to_y").getAsInt(),
                        o.get("to_value").getAsDouble(), y);
            case "minecraft:gradient":
                assertEquals("y", o.get("axis").getAsString());
                return ramp(o.get("from_coordinate").getAsInt(), o.get("from_value").getAsDouble(),
                        o.get("to_coordinate").getAsInt(), o.get("to_value").getAsDouble(), y);
            case "minecraft:interpolated":
                return eval(tree, arg(o, "argument", "input"), y);
            case "minecraft:abs": {
                double[] a = eval(tree, arg(o, "argument", "input"), y);
                double lo = a[0] <= 0.0 && a[1] >= 0.0 ? 0.0 : Math.min(Math.abs(a[0]), Math.abs(a[1]));
                return new double[] {lo, Math.max(Math.abs(a[0]), Math.abs(a[1]))};
            }
            case "minecraft:clamp": {
                double[] a = eval(tree, o.get("input"), y);
                double min = o.get("min").getAsDouble();
                double max = o.get("max").getAsDouble();
                return new double[] {Math.max(min, Math.min(max, a[0])), Math.max(min, Math.min(max, a[1]))};
            }
            case "minecraft:add":
            case "minecraft:mul":
            case "minecraft:min":
            case "minecraft:max": {
                double[] a = eval(tree, arg(o, "argument1", "left"), y);
                double[] b = eval(tree, arg(o, "argument2", "right"), y);
                if (type.endsWith("add")) {
                    return new double[] {a[0] + b[0], a[1] + b[1]};
                }
                if (type.endsWith("min")) {
                    return new double[] {Math.min(a[0], b[0]), Math.min(a[1], b[1])};
                }
                if (type.endsWith("max")) {
                    return new double[] {Math.max(a[0], b[0]), Math.max(a[1], b[1])};
                }
                double p1 = a[0] * b[0];
                double p2 = a[0] * b[1];
                double p3 = a[1] * b[0];
                double p4 = a[1] * b[1];
                return new double[] {Math.min(Math.min(p1, p2), Math.min(p3, p4)),
                        Math.max(Math.max(p1, p2), Math.max(p3, p4))};
            }
            default:
                throw new AssertionError("node type outside the Europa set: " + type);
        }
    }

    private static double[] ramp(int y0, double v0, int y1, double v1, int y) {
        double t = Math.max(0.0, Math.min(1.0, (y - y0) / (double) (y1 - y0)));
        double v = v0 + (v1 - v0) * t;
        return new double[] {v, v};
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("layers: basalt floor, open ocean, solid shell, open sky; the three boundaries vary inside their bands")
    void layers(String tree) throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        JsonElement fin = df(tree, "final_density");
        int top = EuropaNumbers.MIN_Y + EuropaNumbers.HEIGHT;
        for (int y = EuropaNumbers.MIN_Y; y < top; y++) {
            double[] d = eval(tree, fin, y);
            String at = tree + " y=" + y + " [" + d[0] + ", " + d[1] + "]";
            if (y < EuropaNumbers.FLOOR_MIN) {
                assertTrue(d[0] > EPS, "always basalt: " + at);
            } else if (y < EuropaNumbers.FLOOR_MAX) {
                assertTrue(d[0] <= EPS && d[1] > EPS, "the sea floor varies here: " + at);
            } else if (y <= EuropaNumbers.ROOF_MIN) {
                assertTrue(d[1] <= EPS, "always open: " + at);
                assertTrue(y < EuropaNumbers.SEA_LEVEL, "and below sea level, so always water");
            } else if (y <= EuropaNumbers.ROOF_MAX) {
                assertTrue(d[0] <= EPS && d[1] > EPS, "the shell underside varies here: " + at);
            } else if (y < EuropaNumbers.SURFACE_MIN) {
                assertTrue(d[0] > EPS, "always shell: " + at);
            } else if (y < EuropaNumbers.SURFACE_MAX) {
                assertTrue(d[0] <= EPS && d[1] > EPS, "the surface varies here: " + at);
            } else {
                assertTrue(d[1] <= EPS, "always air: " + at);
            }
        }
        // The stated table, literally.
        assertEquals(-45, EuropaNumbers.FLOOR_MIN - 1);
        assertEquals(-20, EuropaNumbers.FLOOR_MAX);
        assertEquals(84, EuropaNumbers.ROOF_MIN);
        assertEquals(101, EuropaNumbers.ROOF_MAX + 1);
        assertEquals(175, EuropaNumbers.SURFACE_MIN - 1);
        assertEquals(212, EuropaNumbers.SURFACE_MAX);
        assertEquals(88, EuropaNumbers.SEA_LEVEL);
        assertTrue(EuropaNumbers.ROOF_MIN - EuropaNumbers.FLOOR_MAX >= 104, "the water column is never shallower");
    }

    private static void ramps(JsonElement node, List<int[]> out) {
        if (node.isJsonObject()) {
            JsonObject o = node.getAsJsonObject();
            String type = o.has("type") ? o.get("type").getAsString() : "";
            if (type.equals("minecraft:y_clamped_gradient")) {
                out.add(new int[] {o.get("from_y").getAsInt(), o.get("to_y").getAsInt()});
            } else if (type.equals("minecraft:gradient")) {
                out.add(new int[] {o.get("from_coordinate").getAsInt(), o.get("to_coordinate").getAsInt()});
            }
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                ramps(e.getValue(), out);
            }
        }
    }

    /** Every noise node's direct parent must be a clamp to [-1, 1]. */
    private static int noisesClamped(JsonElement node, JsonObject parent, String where) {
        if (!node.isJsonObject()) {
            return 0;
        }
        JsonObject o = node.getAsJsonObject();
        int n = 0;
        if ("minecraft:noise".equals(o.get("type").getAsString())) {
            assertTrue(parent != null && "minecraft:clamp".equals(parent.get("type").getAsString())
                    && parent.get("min").getAsDouble() == -1.0 && parent.get("max").getAsDouble() == 1.0,
                    where + ": a noise is not clamped to [-1, 1] before it is scaled");
            assertEquals(0.0, o.get("y_scale").getAsDouble(), where + ": 2D noise only");
            assertTrue(o.get("noise").getAsString().startsWith("nerospace:europa/"), where);
            n++;
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            n += noisesClamped(e.getValue(), o, where);
        }
        return n;
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("ramp ends sit on interpolation cell corners and every terrain noise is clamped before scaling")
    void exactByConstruction(String tree) throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        int noises = 0;
        for (String layer : LAYERS) {
            List<int[]> found = new ArrayList<>();
            ramps(df(tree, layer), found);
            assertEquals(1, found.size(), layer + " has one ramp");
            for (int end : found.get(0)) {
                assertEquals(0, Math.floorMod(end - EuropaNumbers.MIN_Y, 8), tree + " " + layer + " ramp end " + end);
            }
            noises += noisesClamped(df(tree, layer), null, tree + " " + layer);
        }
        assertEquals(4, noises, "surface, rafts, underside, seabed");
        // depth = (192 - y) / 128
        JsonElement depth = df(tree, "depth");
        for (int y : new int[] {-64, EuropaNumbers.BIOME_VENT_Y, EuropaNumbers.BIOME_OCEAN_Y, EuropaNumbers.SEA_LEVEL,
                EuropaNumbers.BIOME_SURFACE_Y, 192, 319}) {
            assertEquals(EuropaNumbers.depthAt(y), eval(tree, depth, y)[0], 1.0E-9, tree + " depth at " + y);
        }
    }

    private static boolean isRef(JsonElement e, String name) {
        return e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()
                && e.getAsString().equals("nerospace:europa/" + name);
    }

    /** The constant that is combined (by {@code type}) with a node satisfying {@code other}, anywhere under node. */
    private static Double constantBeside(JsonElement node, String type, java.util.function.Predicate<JsonElement> other) {
        if (!node.isJsonObject()) {
            return null;
        }
        JsonObject o = node.getAsJsonObject();
        if (type.equals(o.get("type").getAsString())) {
            JsonElement a = arg(o, "argument1", "left");
            JsonElement b = arg(o, "argument2", "right");
            if (a.isJsonPrimitive() && a.getAsJsonPrimitive().isNumber() && other.test(b)) {
                return a.getAsDouble();
            }
            if (b.isJsonPrimitive() && b.getAsJsonPrimitive().isNumber() && other.test(a)) {
                return b.getAsDouble();
            }
        }
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            Double hit = constantBeside(e.getValue(), type, other);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("chaos relief and the lineae trough use the biome table's own thresholds")
    void thresholdsMatchBiomeTable(String tree) throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        JsonElement topside = df(tree, "topside");
        Double erosion = constantBeside(topside, "minecraft:add", e -> isRef(e, "erosion"));
        assertEquals(-EuropaNumbers.CHAOS_EROSION, erosion, 1.0E-9, "erosion threshold");
        Double ridges = constantBeside(topside, "minecraft:mul", e -> e.isJsonObject()
                && "minecraft:abs".equals(e.getAsJsonObject().get("type").getAsString())
                && isRef(arg(e.getAsJsonObject(), "argument", "input"), "ridges"));
        assertEquals(-1.0 / EuropaNumbers.LINEAE_HALF_WIDTH, ridges, 1.0E-9, "ridges scale");
        JsonArray biomes = EuropaFiles.json(EuropaFiles.data("resources", "dimension/europa.json"))
                .getAsJsonObject("generator").getAsJsonObject("biome_source").getAsJsonArray("biomes");
        boolean chaos = false;
        boolean lineae = false;
        for (JsonElement b : biomes) {
            JsonObject p = b.getAsJsonObject().getAsJsonObject("parameters");
            String id = b.getAsJsonObject().get("biome").getAsString();
            if (id.equals("nerospace:europa_chaos")) {
                assertEquals(EuropaNumbers.CHAOS_EROSION, p.getAsJsonArray("erosion").get(0).getAsDouble(), 1.0E-9);
                chaos = true;
            }
            if (id.equals("nerospace:europa_lineae")) {
                assertEquals(-EuropaNumbers.LINEAE_HALF_WIDTH, p.getAsJsonArray("weirdness").get(0).getAsDouble(), 1.0E-9);
                assertEquals(EuropaNumbers.LINEAE_HALF_WIDTH, p.getAsJsonArray("weirdness").get(1).getAsDouble(), 1.0E-9);
                lineae = true;
            }
        }
        assertTrue(chaos && lineae);
    }

    /** One tree's spelling reduced to the other's: argument names, the ramp type, and the interpolation cell size. */
    private static JsonElement normalise(JsonElement node) {
        if (node.isJsonArray()) {
            JsonArray out = new JsonArray();
            node.getAsJsonArray().forEach(e -> out.add(normalise(e)));
            return out;
        }
        if (!node.isJsonObject()) {
            return node;
        }
        JsonObject o = node.getAsJsonObject();
        JsonObject out = new JsonObject();
        boolean gradient = o.has("type") && "minecraft:gradient".equals(o.get("type").getAsString());
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String k = e.getKey();
            if (k.startsWith("cell_size_") || (gradient && k.equals("axis"))) {
                continue;
            }
            if (k.equals("type") && gradient) {
                out.addProperty("type", "minecraft:y_clamped_gradient");
                continue;
            }
            String key = switch (k) {
                case "left" -> "argument1";
                case "right" -> "argument2";
                case "input" -> "argument";
                case "from_coordinate" -> "from_y";
                case "to_coordinate" -> "to_y";
                default -> k;
            };
            out.add(key, normalise(e.getValue()));
        }
        return out;
    }

    @Test
    @DisplayName("the two trees hold the same density functions, differing only in spelling")
    void treesAreIsomorphic() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        for (String name : List.of("topside", "underside", "seabed", "final_density", "depth", "continents", "erosion",
                "ridges", "temperature", "vegetation")) {
            assertEquals(normalise(df("resources", name)), normalise(df("resources-26.3", name)), name);
        }
        for (String noise : List.of("surface", "rafts", "underside", "seabed", "continentalness", "erosion", "ridge",
                "temperature", "vegetation")) {
            for (String tree : EuropaFiles.TREES) {
                assertTrue(java.nio.file.Files.isRegularFile(
                        EuropaFiles.data(tree, "worldgen/noise/europa/" + noise + ".json")), tree + " " + noise);
            }
        }
        assertFalse(java.nio.file.Files.exists(EuropaFiles.data("resources", DF + "chunk_surface_level.json")),
                "the surface-level function is a 26.3 file only");
        assertTrue(java.nio.file.Files.isRegularFile(EuropaFiles.data("resources-26.3", DF + "chunk_surface_level.json")));
    }
}
