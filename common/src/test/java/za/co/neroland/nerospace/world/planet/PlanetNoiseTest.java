package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Per-planet terrain noise. Vanilla seeds each noise from its registry id, so Luna, Mars, Nyxara and Cindara read their
 * own copies ({@code nerospace:<planet>/...}) and never generate the Overworld's (or each other's) heightmap for the
 * same seed. Glacira and Greenxertz keep the shared router on purpose (terrain continuity across old chunks).
 */
class PlanetNoiseTest {

    private static final Pattern REF = Pattern.compile("\"([a-z0-9_]+:[a-z0-9_/]+)\"");
    private static final String[] TREES = {"resources", "resources-26.3"};

    static Path main() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("common/src/main");
            if (Files.isDirectory(c)) {
                return c;
            }
        }
        return null;
    }

    private static JsonObject json(Path f) throws IOException {
        return JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static Path worldgen(String tree, String rel) {
        return main().resolve(tree).resolve("data/nerospace/worldgen").resolve(rel);
    }

    @ParameterizedTest
    @ValueSource(strings = {"luna", "mars", "nyxara", "cindara"})
    @DisplayName("the terrain router reads only the planet's own noise copies, in both trees")
    void ownNoise(String planet) throws IOException {
        Assumptions.assumeTrue(main() != null, "common sources not found");
        for (String tree : TREES) {
            String router = json(worldgen(tree, "noise_settings/" + planet + ".json")).get("noise_router").toString();
            for (String key : new String[] {"continents", "erosion", "ridges", "depth"}) {
                assertTrue(router.contains("\"" + key + "\":\"nerospace:" + planet + "/overworld/" + key + "\""),
                        tree + " " + planet + " " + key);
            }
            assertFalse(router.contains("minecraft:overworld/continents"), tree + " " + planet);
            // Every planet-owned id the router (transitively) reaches exists in this tree and stays in the planet.
            Set<String> seen = new TreeSet<>();
            walk(tree, planet, router, seen);
            assertTrue(seen.stream().anyMatch(s -> s.endsWith("/continentalness")), tree + " " + planet + " " + seen);
        }
    }

    private void walk(String tree, String planet, String json, Set<String> seen) throws IOException {
        Matcher m = REF.matcher(json);
        while (m.find()) {
            String id = m.group(1);
            if (!id.startsWith("nerospace:" + planet + "/")) {
                assertFalse(id.startsWith("nerospace:") && id.contains("/") && !id.startsWith("nerospace:" + planet),
                        "cross-planet noise ref " + id);
                continue;
            }
            if (!seen.add(id)) {
                continue;
            }
            String path = id.substring("nerospace:".length());
            Path df = worldgen(tree, "density_function/" + path + ".json");
            Path noise = worldgen(tree, "noise/" + path + ".json");
            assertTrue(Files.isRegularFile(df) || Files.isRegularFile(noise), tree + ": missing " + id);
            if (Files.isRegularFile(df)) {
                walk(tree, planet, Files.readString(df, StandardCharsets.UTF_8), seen);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"glacira", "greenxertz"})
    @DisplayName("Glacira and Greenxertz keep the shared router (terrain continues across the old-chunk seam)")
    void sharedRouterKept(String planet) throws IOException {
        Assumptions.assumeTrue(main() != null, "common sources not found");
        for (String tree : TREES) {
            String router = json(worldgen(tree, "noise_settings/" + planet + ".json")).get("noise_router").toString();
            assertFalse(router.contains("nerospace:" + planet + "/"), tree + " " + planet);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"density_function", "noise"})
    @DisplayName("every shared-tree noise / density function has a 26.3 twin (the shared tree also loads on 26.3)")
    void sharedTreeMirroredOn263(String kind) throws IOException {
        Assumptions.assumeTrue(main() != null, "common sources not found");
        Path base = worldgen("resources", kind);
        Assumptions.assumeTrue(Files.isDirectory(base));
        try (Stream<Path> files = Files.walk(base)) {
            for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                String rel = base.relativize(f).toString().replace('\\', '/');
                assertTrue(Files.isRegularFile(worldgen("resources-26.3", kind + "/" + rel)), kind + "/" + rel);
            }
        }
        assertEquals(base.getFileName().toString(), kind);
    }
}
