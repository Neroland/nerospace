package za.co.neroland.nerospace.block;

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
 * The cheese stays small. The string {@code moon_cheese} may appear only in the cheese's own files
 * (ore + wheel loot, the wheel recipe, the hidden advancement and guide step, lang, the materials highlight tag, its
 * item model/definition, the registries and Luna's own package). Scans {@code common/src/main} text files.
 */
class CheeseIsolationTest {

    private static final String NEEDLE = "moon" + "_cheese";

    private static final List<Pattern> ALLOWED = List.of(
            Pattern.compile("resources/assets/nerospace/lang/[a-z_]+\\.json"),
            Pattern.compile("resources(-[0-9.]+)?/data/nerospace/loot_table/blocks/cheese_(ore|wheel)\\.json"),
            Pattern.compile("resources/data/nerospace/recipe/cheese_wheel\\.json"),
            Pattern.compile("resources/data/nerospace/advancement/luna/the_big_cheese\\.json"),
            Pattern.compile("resources/data/neroland/tags/item/highlight/materials\\.json"),
            Pattern.compile("resources/assets/nerospace/(items|models/item)/moon_cheese\\.json"),
            Pattern.compile("resources/.*guide.*big_cheese.*"),
            Pattern.compile("java/za/co/neroland/nerospace/registry/Mod(Items|Blocks|CreativeTab)\\.java"),
            Pattern.compile("java/za/co/neroland/nerospace/world/planet/luna/.*"));

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

    @Test
    @DisplayName("moon_cheese appears only in the cheese's own files")
    void isolated() throws IOException {
        Path root = commonMain();
        Assumptions.assumeTrue(root != null, "common/src/main not found from " + System.getProperty("user.dir"));
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().matches(".*\\.(java|json|mcmeta|md|txt|properties)$"))::iterator) {
                String rel = root.relativize(f).toString().replace('\\', '/');
                if (ALLOWED.stream().anyMatch(a -> a.matcher(rel).matches())) {
                    continue;
                }
                if (Files.readString(f, StandardCharsets.UTF_8).contains(NEEDLE)) {
                    offenders.add(rel);
                }
            }
        }
        assertTrue(offenders.isEmpty(), "cheese leaked into " + offenders);
    }
}
