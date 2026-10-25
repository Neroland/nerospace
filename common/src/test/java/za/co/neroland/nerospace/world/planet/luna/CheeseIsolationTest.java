package za.co.neroland.nerospace.world.planet.luna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The cheese stays small: the word appears only in the cheese's own files, and the wheel is the only
 * recipe that mentions it. Vanilla's {@code cave_cheese} / {@code sloped_cheese} terrain noises are not ours and is ignored.
 */
class CheeseIsolationTest {

    private static final Pattern WORD = Pattern.compile("(?<!cave_|sloped_)cheese", Pattern.CASE_INSENSITIVE);

    private static final List<Pattern> ALLOWED = List.of(
            Pattern.compile("resources[^/]*/assets/nerospace/lang/[a-z_]+\\.json"),
            Pattern.compile("resources[^/]*/assets/nerospace/(blockstates|items|models/block|models/item)/(cheese_(ore|wheel)|moon_cheese)[a-z0-9_]*\\.json"),
            Pattern.compile("resources[^/]*/assets/nerospace/textures/(block|item)/(cheese_|moon_cheese).*"),
            Pattern.compile("resources[^/]*/data/nerospace/loot_table/blocks/cheese_(ore|wheel)\\.json"),
            Pattern.compile("resources[^/]*/data/nerospace/recipe/cheese_wheel\\.json"),
            Pattern.compile("resources[^/]*/data/nerospace/advancement/luna/the_big_cheese\\.json"),
            Pattern.compile("resources[^/]*/data/nerospace/worldgen/(configured_feature|feature|placed_feature)/luna_cheese_ore\\.json"),
            Pattern.compile("resources[^/]*/data/nerospace/worldgen/biome/luna_crater_field\\.json"),
            Pattern.compile("resources[^/]*/data/(neroland|minecraft|c)/tags/.*\\.json"),
            Pattern.compile("resources[^/]*/.*guide.*big_cheese.*"),
            Pattern.compile("java/za/co/neroland/nerospace/block/rules/CheeseRules\\.java"),
            Pattern.compile("java/za/co/neroland/nerospace/registry/Mod(Items|Blocks|CreativeTab|Registries)\\.java"),
            Pattern.compile("java/za/co/neroland/nerospace/progression/StarGuide.*\\.java"),
            Pattern.compile("java/za/co/neroland/nerospace/world/planet/luna/.*"));

    @Test
    @DisplayName("cheese appears only in the cheese's own files")
    void isolated() throws IOException {
        Path root = LunaFiles.commonMain();
        Assumptions.assumeTrue(root != null, "common/src/main not found");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().matches(".*\\.(java|json|mcmeta|md|txt|properties|toml)$"))::iterator) {
                String rel = root.relativize(f).toString().replace('\\', '/');
                if (ALLOWED.stream().anyMatch(a -> a.matcher(rel).matches())) {
                    continue;
                }
                if (WORD.matcher(Files.readString(f, StandardCharsets.UTF_8)).find()) {
                    offenders.add(rel);
                }
            }
        }
        assertTrue(offenders.isEmpty(), "cheese leaked into " + offenders);
    }

    @Test
    @DisplayName("the Cheese Wheel is the only recipe that uses cheese")
    void onlyTheWheelRecipe() throws IOException {
        Path root = LunaFiles.commonMain();
        Assumptions.assumeTrue(root != null, "common/src/main not found");
        List<String> recipes = new ArrayList<>();
        for (String tree : List.of("resources", "resources-26.3")) {
            Path dir = root.resolve(tree).resolve("data");
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().replace('\\', '/').contains("/recipe/"))::iterator) {
                    if (WORD.matcher(Files.readString(f, StandardCharsets.UTF_8)).find()) {
                        recipes.add(f.getFileName().toString());
                    }
                }
            }
        }
        assertEquals(List.of("cheese_wheel.json"), recipes);
    }
}
