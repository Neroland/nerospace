package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Europa's hidden find stays small (amendments F31, design 11): the element's name appears only in its own files,
 * three recipes mention it, and no trade, machine or highlight tag picks it up. The pattern of the Luna isolation
 * test. Also checks that no Europa file borrows Luna's joke word.
 */
class EuropiumIsolationTest {

    private static final Pattern WORD = Pattern.compile("europium", Pattern.CASE_INSENSITIVE);
    /** Luna's word, which must stay out of every Europa file (vanilla's two terrain noises excepted). */
    private static final Pattern LUNA_WORD = Pattern.compile("(?<!cave_|sloped_)ch" + "eese", Pattern.CASE_INSENSITIVE);
    private static final String TEXT = ".*\\.(java|json|mcmeta|md|txt|properties|toml)$";

    /** Path regexes relative to {@code common/src/main/} (amendments section 7, WP5). */
    private static final List<Pattern> ALLOWED = List.of(
            Pattern.compile("resources[^/]*/assets/nerospace/lang/[^/]+\\.json"),
            Pattern.compile("resources[^/]*/assets/nerospace/(blockstates|items|models/(block|item)|textures/(block|item))/(europium_ore|europium_lamp|raw_europium)[^/]*"),
            Pattern.compile("resources[^/]*/data/nerospace/loot_table/blocks/(europium_ore|europium_lamp)\\.json"),
            Pattern.compile("resources[^/]*/data/nerospace/recipe/(europium_lamp|raw_europium_from_europium_lamp|raw_europium_from_smelting_europium_ore)\\.json"),
            Pattern.compile("resources[^/]*/data/nerospace/advancement/europa/rare_earth\\.json"),
            Pattern.compile("resources[^/]*/data/nerospace/worldgen/(configured_feature|feature)/europa_europium_ore\\.json"),
            Pattern.compile("resources[^/]*/data/nerospace/worldgen/placed_feature/europa_europium_ore_placed\\.json"),
            Pattern.compile("resources[^/]*/data/nerospace/worldgen/biome/europa_vent_fields\\.json"),
            Pattern.compile("resources[^/]*/data/(minecraft|c)/tags/.*\\.json"),
            Pattern.compile("java/za/co/neroland/nerospace/block/rules/PhosphorRules\\.java"),
            Pattern.compile("java/za/co/neroland/nerospace/registry/Mod(Items|Blocks|CreativeTab|Registries)\\.java"),
            Pattern.compile("java/za/co/neroland/nerospace/progression/StarGuide[^/]*\\.java"),
            Pattern.compile("java/za/co/neroland/nerospace/world/planet/europa/.*"));

    private static final Set<String> RECIPES = Set.of("europium_lamp.json", "raw_europium_from_europium_lamp.json",
            "raw_europium_from_smelting_europium_ore.json");

    private static Path root() {
        Path root = EuropaFiles.commonMain();
        Assumptions.assumeTrue(root != null, "common/src/main not found");
        return root;
    }

    private static List<Path> textFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(Files::isRegularFile).filter(p -> p.toString().matches(TEXT)).toList();
        }
    }

    private static String read(Path f) throws IOException {
        return Files.readString(f, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("the element's name appears only in the allow-listed files")
    void isolated() throws IOException {
        Path root = root();
        List<String> offenders = new ArrayList<>();
        for (Path f : textFiles(root)) {
            String rel = root.relativize(f).toString().replace('\\', '/');
            if (ALLOWED.stream().anyMatch(a -> a.matcher(rel).matches())) {
                continue;
            }
            if (WORD.matcher(read(f)).find()) {
                offenders.add(rel);
            }
        }
        assertTrue(offenders.isEmpty(), "europium leaked into " + offenders);
    }

    @Test
    @DisplayName("exactly the three europium recipes mention it, and no recipe unlock advancement does")
    void onlyEuropiumRecipes() throws IOException {
        Path root = root();
        Set<String> recipes = new TreeSet<>();
        List<String> unlocks = new ArrayList<>();
        try (Stream<Path> trees = Files.list(root)) {
            for (Path tree : trees.filter(p -> p.getFileName().toString().startsWith("resources")).toList()) {
                for (Path f : textFiles(tree.resolve("data"))) {
                    String rel = f.toString().replace('\\', '/');
                    if (!WORD.matcher(read(f)).find()) {
                        continue;
                    }
                    if (rel.contains("/recipe/")) {
                        recipes.add(f.getFileName().toString());
                    }
                    if (rel.contains("/advancement/recipes/")) {
                        unlocks.add(root.relativize(f).toString());
                    }
                }
            }
        }
        assertEquals(new TreeSet<>(RECIPES), recipes);
        assertTrue(unlocks.isEmpty(), "recipe unlocks spoil the find: " + unlocks);
    }

    @Test
    @DisplayName("no alien trade uses it")
    void notInTrades() throws IOException {
        Path src = root().resolve("java/za/co/neroland/nerospace/village/AlienTrades.java");
        assertFalse(WORD.matcher(read(src)).find());
    }

    @Test
    @DisplayName("no machine takes it")
    void notInMachines() throws IOException {
        Path machine = root().resolve("java/za/co/neroland/nerospace/machine");
        for (String name : List.of("GrinderRecipes", "GrinderRows", "RefineryRules", "CombustionGeneratorBlockEntity",
                "GeneratorFuels", "FuelRefineryBlockEntity")) {
            Path src = machine.resolve(name + ".java");
            assertTrue(Files.isRegularFile(src), name);
            assertFalse(WORD.matcher(read(src)).find(), name);
        }
    }

    @Test
    @DisplayName("it is not highlighted as a material")
    void notHighlighted() throws IOException {
        for (String tree : EuropaFiles.TREES) {
            Path tag = root().resolve(tree).resolve("data/neroland/tags/item/highlight/materials.json");
            if (Files.isRegularFile(tag)) {
                assertFalse(WORD.matcher(read(tag)).find(), tree);
            }
        }
    }

    @Test
    @DisplayName("no Europa file uses Luna's joke word")
    void noLunaWord() throws IOException {
        Path root = root();
        List<String> offenders = new ArrayList<>();
        for (Path f : textFiles(root)) {
            String rel = root.relativize(f).toString().replace('\\', '/');
            String name = f.getFileName().toString();
            boolean europa = rel.contains("/planet/europa/") || name.contains("europa") || name.contains("europium")
                    || name.contains("brinekin");
            if (europa && LUNA_WORD.matcher(read(f)).find()) {
                offenders.add(rel);
            }
        }
        assertTrue(offenders.isEmpty(), "Luna's word in Europa files: " + offenders);
    }
}
