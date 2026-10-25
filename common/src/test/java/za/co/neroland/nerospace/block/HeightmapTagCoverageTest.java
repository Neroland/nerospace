package za.co.neroland.nerospace.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * 26.3 heightmap tag rule (docs/MULTILOADER.md). From Minecraft 26.3 the MOTION_BLOCKING, MOTION_BLOCKING_NO_LEAVES
 * and OCEAN_FLOOR(_WG) heightmaps count only blocks in {@code #minecraft:blocks_motion_in_heightmap(_no_leaves)},
 * which resolve to {@code #minecraft:blocks_motion_no_leaves}. A solid Nerospace block missing from that tag is
 * invisible to terrain height checks on 26.3 (rain falls through it, mobs spawn at the wrong height, ores placed
 * below OCEAN_FLOOR_WG never generate).
 *
 * <p>Every registered Nerospace block must therefore be listed in the 26.3-only overlay
 * {@code resources-26.3/data/minecraft/tags/block/blocks_motion_no_leaves.json}, unless it is in {@link #EXCLUDED}.
 * Block ids are discovered without bootstrapping the game: every {@code assets/nerospace/blockstates/*.json} plus every
 * string-literal id passed to {@code BLOCKS.register("...")} / {@code block("...")} in the registration sources.
 */
class HeightmapTagCoverageTest {

    private static final String TAG = "resources-26.3/data/minecraft/tags/block/blocks_motion_no_leaves.json";

    /**
     * Intentional exclusions, following vanilla 26.3 precedent (vanilla lists solid blocks, glass, panes, fences,
     * chains, lanterns, pressure plates and amethyst clusters, but not fluids, plants, snow layers or carpets).
     */
    private static final Map<String, String> EXCLUDED = Map.of(
            // Greenxertz plants/carpet (no collision box, like vanilla plants and carpets)
            "spore_moss", "1/16 moss carpet that spreads - vanilla excludes carpets",
            "glowcap_fungus", "mushroom-like plant, no collision - vanilla excludes mushrooms",
            "xertz_vine", "hanging vine, no collision - vanilla excludes vines",
            "spore_pod", "pod plant, no collision (bursts on touch) - vanilla excludes plants",
            "rocket_fuel", "fluid block (no collision) - vanilla excludes water/lava",
            "ash_layer", "snow-like weather layer (forceSolidOff) - vanilla excludes minecraft:snow layers",
            // Nyxara: glow-lichen-style multiface, noCollision - vanilla 26.3 excludes minecraft:glow_lichen
            "shadow_lichen", "multiface carpet (noCollision) - vanilla excludes minecraft:glow_lichen");

    private static final Pattern REGISTER = Pattern.compile("(?:BLOCKS\\.register|\\bblock)\\(\\s*\"([a-z0-9_]+)\"");

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

    private static Set<String> registeredBlocks(Path root) throws IOException {
        Set<String> ids = new TreeSet<>();
        try (Stream<Path> files = Files.list(root.resolve("resources/assets/nerospace/blockstates"))) {
            files.map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".json"))
                    .forEach(n -> ids.add(n.substring(0, n.length() - ".json".length())));
        }
        try (Stream<Path> files = Files.walk(root.resolve("java"))) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String src = Files.readString(f, StandardCharsets.UTF_8);
                if (!src.contains("BLOCKS")) {
                    continue;
                }
                Matcher m = REGISTER.matcher(src);
                while (m.find()) {
                    ids.add(m.group(1));
                }
            }
        }
        return ids;
    }

    private static List<String> tagValues(Path root) throws IOException {
        List<String> values = new ArrayList<>();
        for (JsonElement e : JsonParser.parseString(Files.readString(root.resolve(TAG), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonArray("values")) {
            values.add(e.getAsString());
        }
        return values;
    }

    @Test
    @DisplayName("every solid Nerospace block is in the 26.3 blocks_motion_no_leaves tag")
    void everySolidBlockTagged() throws IOException {
        Path root = commonMain();
        Assumptions.assumeTrue(root != null, "common/src/main not found from " + System.getProperty("user.dir"));
        Set<String> registered = registeredBlocks(root);
        assertTrue(registered.size() > 50, "block discovery found too few ids: " + registered);
        Set<String> tagged = new TreeSet<>(tagValues(root));
        List<String> missing = new ArrayList<>();
        for (String id : registered) {
            if (!EXCLUDED.containsKey(id) && !tagged.contains("nerospace:" + id)) {
                missing.add("nerospace:" + id);
            }
        }
        assertTrue(missing.isEmpty(), "add to " + TAG + " (or to EXCLUDED with a reason): " + missing);
    }

    @Test
    @DisplayName("the 26.3 heightmap tag has no stale, excluded or duplicate entries")
    void tagIsClean() throws IOException {
        Path root = commonMain();
        Assumptions.assumeTrue(root != null, "common/src/main not found from " + System.getProperty("user.dir"));
        Set<String> registered = registeredBlocks(root);
        List<String> values = tagValues(root);
        assertTrue(values.size() == new TreeSet<>(values).size(), "duplicate entries in " + TAG);
        for (String v : values) {
            String id = v.startsWith("nerospace:") ? v.substring("nerospace:".length()) : null;
            assertTrue(id != null && registered.contains(id), "stale/foreign entry in " + TAG + ": " + v);
            assertFalse(EXCLUDED.containsKey(id), "excluded block is tagged: " + v);
        }
    }

    @Test
    @DisplayName("the heightmap tag overlay stays 26.3-only")
    void overlayIsVersionScoped() {
        Path root = commonMain();
        Assumptions.assumeTrue(root != null, "common/src/main not found from " + System.getProperty("user.dir"));
        assertFalse(Files.exists(root.resolve("resources/data/minecraft/tags/block/blocks_motion_no_leaves.json")),
                "26.1.2/26.2 heightmaps do not read this tag; keep it in resources-26.3 only");
    }
}
