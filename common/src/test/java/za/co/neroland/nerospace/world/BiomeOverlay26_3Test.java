package za.co.neroland.nerospace.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 26.3 biome format rule (docs/MULTILOADER.md). Minecraft 26.3 reads a biome's mob spawns only from the
 * {@code minecraft:gameplay/natural_mob_spawns} attribute and silently ignores the older top-level {@code spawners} /
 * {@code spawn_costs} fields, so a shared biome without a 26.3 copy spawns nothing on 26.3. Every shared biome must
 * have a {@code resources-26.3} copy written by {@code tools/gen_biome_overlays.py}, with the same spawns.
 */
class BiomeOverlay26_3Test {

    private static final String BIOMES = "data/nerospace/worldgen/biome";
    private static final String ATTRIBUTE = "minecraft:gameplay/natural_mob_spawns";

    private static Path commonMain() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("common/src/main");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Map<String, JsonObject> biomes(Path dir) throws IOException {
        Map<String, JsonObject> out = new TreeMap<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                String rel = dir.relativize(f).toString().replace('\\', '/');
                out.put(rel, JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject());
            }
        }
        return out;
    }

    /** One "category type weight min..max" line per spawn entry, old format. */
    private static List<String> baseSpawns(JsonObject biome) {
        List<String> out = new ArrayList<>();
        JsonObject spawners = biome.has("spawners") ? biome.getAsJsonObject("spawners") : new JsonObject();
        for (Map.Entry<String, JsonElement> category : spawners.entrySet()) {
            for (JsonElement e : category.getValue().getAsJsonArray()) {
                JsonObject s = e.getAsJsonObject();
                out.add(category.getKey() + " " + s.get("type").getAsString() + " w" + s.get("weight").getAsInt()
                        + " " + s.get("minCount").getAsInt() + ".." + s.get("maxCount").getAsInt());
            }
        }
        out.sort(null);
        return out;
    }

    /** The same lines, read from the 26.3 attribute. */
    private static List<String> overlaySpawns(JsonObject biome, String name) {
        List<String> out = new ArrayList<>();
        JsonObject attributes = biome.getAsJsonObject("attributes");
        assertTrue(attributes != null && attributes.has(ATTRIBUTE), name + ": no " + ATTRIBUTE);
        JsonObject argument = attributes.getAsJsonObject(ATTRIBUTE).getAsJsonObject("argument");
        assertTrue(argument != null && argument.has("spawns_by_category"), name + ": no spawns_by_category");
        for (Map.Entry<String, JsonElement> category : argument.getAsJsonObject("spawns_by_category").entrySet()) {
            for (JsonElement e : category.getValue().getAsJsonArray()) {
                JsonObject s = e.getAsJsonObject();
                JsonElement count = s.get("count");
                int min;
                int max;
                if (count.isJsonPrimitive()) {
                    min = max = count.getAsInt();
                } else {
                    JsonObject provider = count.getAsJsonObject();
                    assertEquals("minecraft:uniform", provider.get("type").getAsString(), name + ": count provider");
                    min = provider.get("min_inclusive").getAsInt();
                    max = provider.get("max_inclusive").getAsInt();
                }
                out.add(category.getKey() + " " + s.get("type").getAsString() + " w" + s.get("weight").getAsInt()
                        + " " + min + ".." + max);
            }
        }
        out.sort(null);
        return out;
    }

    @Test
    @DisplayName("every biome has a 26.3 copy with the same spawns and no old spawn fields")
    void everyBiomeHasA263Overlay() throws IOException {
        Path root = commonMain();
        Assumptions.assumeTrue(root != null, "common/src/main not found");
        Map<String, JsonObject> base = biomes(root.resolve("resources").resolve(BIOMES));
        Map<String, JsonObject> overlay = biomes(root.resolve("resources-26.3").resolve(BIOMES));
        assertFalse(base.isEmpty(), "no shared biomes found");
        List<String> missing = new ArrayList<>(base.keySet());
        missing.removeAll(overlay.keySet());
        assertTrue(missing.isEmpty(), "no 26.3 overlay (run tools/gen_biome_overlays.py) for " + missing);
        List<String> orphans = new ArrayList<>(overlay.keySet());
        orphans.removeAll(base.keySet());
        assertTrue(orphans.isEmpty(), "26.3 overlay without a shared biome: " + orphans);
        for (Map.Entry<String, JsonObject> e : base.entrySet()) {
            String name = e.getKey();
            JsonObject o = overlay.get(name);
            assertFalse(o.has("spawners"), name + ": 26.3 overlay still has 'spawners' (26.3 ignores it)");
            assertFalse(o.has("spawn_costs"), name + ": 26.3 overlay still has 'spawn_costs' (26.3 ignores it)");
            assertEquals(baseSpawns(e.getValue()), overlaySpawns(o, name),
                    name + ": 26.3 spawns differ from the shared biome (run tools/gen_biome_overlays.py)");
        }
    }

    @Test
    @DisplayName("the alien villager spawns in Greenxertz on 26.3")
    void greenxertzKeepsItsVillagers() throws IOException {
        Path root = commonMain();
        Assumptions.assumeTrue(root != null, "common/src/main not found");
        JsonObject o = biomes(root.resolve("resources-26.3").resolve(BIOMES)).get("greenxertz.json");
        assertTrue(o != null, "no 26.3 greenxertz biome");
        JsonArray creatures = o.getAsJsonObject("attributes").getAsJsonObject(ATTRIBUTE).getAsJsonObject("argument")
                .getAsJsonObject("spawns_by_category").getAsJsonArray("creature");
        assertTrue(creatures.toString().contains("\"nerospace:alien_villager\""), "alien villager missing: " + creatures);
    }
}
