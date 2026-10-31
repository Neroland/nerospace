package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Beds on every Nerospace world: each dimension type (shared, shared 26.3 and NeoForge 26.3 copies) declares a bed
 * rule that always sets the respawn point, so a bed never explodes and always works as a spawn point. Greenxertz keeps
 * vanilla's {@code when_dark} (its day is the vanilla day); every other world lets the bed be used at any time and the
 * planet sleep rules decide when sleeping is allowed. The planet rules carry no {@code error_message}: the planet's own
 * action-bar line is the refusal (a vanilla message would print over it on Fabric).
 */
class BedRuleCoverageTest {

    private static final String RULE = "minecraft:gameplay/bed_rule";

    @Test
    @DisplayName("every dimension type declares a bed rule that always sets spawn; planets sleep by the planet clock")
    void everyDimensionTypeHasTheBedRule() throws IOException {
        Path main = PlanetNoiseTest.main();
        Assumptions.assumeTrue(main != null, "common sources not found");
        Path repo = main.getParent().getParent().getParent();
        List<Path> files = new ArrayList<>();
        for (Path dir : new Path[] {main.resolve("resources/data/nerospace/dimension_type"),
                main.resolve("resources-26.3/data/nerospace/dimension_type"),
                repo.resolve("neoforge/src/main/resources-26.3/data/nerospace/dimension_type")}) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> s = Files.list(dir)) {
                s.filter(p -> p.toString().endsWith(".json")).sorted().forEach(files::add);
            }
        }
        assertTrue(files.size() >= 14, "expected the shared and NeoForge 26.3 dimension types, found " + files);
        for (Path f : files) {
            JsonObject attrs = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonObject("attributes");
            assertNotNull(attrs, f + " has no attributes");
            JsonObject rule = attrs.getAsJsonObject(RULE);
            assertNotNull(rule, f + " has no " + RULE);
            assertEquals("always", rule.get("can_set_spawn").getAsString(), f.toString());
            if (f.getFileName().toString().equals("greenxertz.json")) {
                assertEquals("when_dark", rule.get("can_sleep").getAsString(), f.toString());
            } else {
                assertEquals("always", rule.get("can_sleep").getAsString(), f.toString());
                assertFalse(rule.has("error_message"), f + ": the planet sleep rules send their own line");
                assertFalse(rule.has("destroy_on_use") && rule.get("destroy_on_use").getAsBoolean(), f.toString());
            }
        }
    }
}
