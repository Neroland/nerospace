package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

/**
 * Every fauna id in every terraform table is a registered Nerospace entity, so TerraformFauna never
 * stocks nothing; and every Living biome lists its planet's species as a natural spawn (26.3 overlay included).
 */
class TerraformFaunaTableTest {

    private static final Pattern REGISTER = Pattern.compile("ENTITY_TYPES\\.register\\(\\s*\"([a-z0-9_]+)\"");

    private static Path commonMain() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("common/src/main");
            if (Files.isDirectory(c)) {
                return c;
            }
        }
        throw new IllegalStateException("common/src/main not found");
    }

    /** Every entity path registered through the shared ENTITY_TYPES provider, from the sources. */
    private static Set<String> registeredEntityIds() throws IOException {
        Set<String> out = new HashSet<>();
        try (Stream<Path> files = Files.walk(commonMain().resolve("java"))) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                Matcher m = REGISTER.matcher(Files.readString(f, StandardCharsets.UTF_8));
                while (m.find()) {
                    out.add(m.group(1));
                }
            }
        }
        return out;
    }

    @Test
    @DisplayName("every terraform table's fauna id is a registered nerospace entity")
    void faunaRegistered() throws IOException {
        Set<String> registered = registeredEntityIds();
        assertTrue(registered.contains("woolly_drift"), "scanner sanity: " + registered);
        for (TerraformTable t : TerraformTables.ORDER) {
            assertFalse(t.fauna().isEmpty(), t.langId() + " has a fauna set");
            for (Identifier id : t.fauna()) {
                assertTrue(id.getNamespace().equals("nerospace") && registered.contains(id.getPath()),
                        t.langId() + " fauna " + id + " is not a registered entity");
            }
        }
    }

    @Test
    @DisplayName("Luna, Mars and Nyxara Living biomes spawn their species on both resource trees")
    void livingBiomesSpawnTheirFauna() throws IOException {
        for (TerraformTable t : new TerraformTable[] {
                za.co.neroland.nerospace.world.planet.luna.LunaTerraform.TABLE,
                za.co.neroland.nerospace.world.planet.mars.MarsTerraform.TABLE,
                za.co.neroland.nerospace.world.planet.nyxara.NyxaraTerraform.TABLE}) {
            String biome = t.living().identifier().getPath();
            String species = t.fauna().get(0).toString();
            for (String tree : new String[] {"resources", "resources-26.3"}) {
                String text = Files.readString(commonMain().resolve(tree + "/data/nerospace/worldgen/biome/"
                        + biome + ".json"), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                assertTrue(json.toString().contains("\"type\":\"" + species + "\""), tree + " " + biome
                        + " spawns " + species);
            }
        }
    }
}
