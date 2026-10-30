package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Planet loot numbers that are easy to get wrong in one of the two resource trees. */
class PlanetLootTest {

    private static String loot(String tree, String id) throws IOException {
        Path main = PlanetNoiseTest.main();
        Assumptions.assumeTrue(main != null, "common sources not found");
        return Files.readString(main.resolve(tree).resolve("data/nerospace/loot_table/blocks/" + id + ".json"),
                StandardCharsets.UTF_8).replaceAll("\\s", "");
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("a grown Basalt Column never multiplies items: only segment 0 drops it (Silk Touch for 1-3)")
    void basaltColumnSegments(String tree) throws IOException {
        String s = loot(tree, "basalt_column");
        assertTrue(s.contains("\"segment\":\"0\""), s);
        assertTrue(s.contains("silk_touch"), s);
        assertTrue(s.contains("any_of"), s);
    }

    @ParameterizedTest
    @ValueSource(strings = {"resources", "resources-26.3"})
    @DisplayName("Methane Ice drops 1-3 crystals (Fortune ore_drops)")
    void methaneIce(String tree) throws IOException {
        String s = loot(tree, "methane_ice");
        assertTrue(s.contains("\"min\":1") && s.contains("\"max\":3"), s);
        assertFalse(s.contains("\"max\":4"), s);
        assertTrue(s.contains("ore_drops"), s);
    }
}
