package za.co.neroland.nerospace.world.planet;

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
 * Minecraft 26.3 reads noise parameters as {@code base_octave} / {@code base_amplitude} / {@code octave_count} /
 * {@code amplitude_modifiers}; the older {@code firstOctave} / {@code amplitudes} spelling fails the whole registry load,
 * and with it every world. Each tree must carry its own spelling.
 */
class NoiseFormat26_3Test {

    private static List<Path> noises(String tree) throws IOException {
        Path main = PlanetNoiseTest.main();
        Assumptions.assumeTrue(main != null, "common sources not found");
        Path dir = main.resolve(tree).resolve("data/nerospace/worldgen/noise");
        List<Path> out = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            files.filter(p -> p.toString().endsWith(".json")).forEach(out::add);
        }
        return out;
    }

    private static JsonObject json(Path f) throws IOException {
        return JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    @DisplayName("every 26.3 noise uses the 26.3 spelling")
    void tree263() throws IOException {
        List<Path> files = noises("resources-26.3");
        assertTrue(files.size() > 50, "26.3 noise files found: " + files.size());
        for (Path f : files) {
            JsonObject o = json(f);
            assertTrue(o.has("base_octave"), f + " lacks base_octave");
            assertTrue(!o.has("firstOctave") && !o.has("amplitudes"), f + " uses the pre-26.3 spelling");
            if (o.has("amplitude_modifiers")) {
                int count = o.has("octave_count") ? o.get("octave_count").getAsInt() : 1;
                assertTrue(o.getAsJsonArray("amplitude_modifiers").size() == count,
                        f + " amplitude_modifiers size differs from octave_count");
            }
        }
    }

    @Test
    @DisplayName("every 26.1/26.2 noise uses the older spelling")
    void baseTree() throws IOException {
        for (Path f : noises("resources")) {
            JsonObject o = json(f);
            assertTrue(o.has("firstOctave") && o.has("amplitudes"), f + " is not in the 26.1/26.2 spelling");
        }
    }
}
