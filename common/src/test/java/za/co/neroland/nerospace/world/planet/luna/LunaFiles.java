package za.co.neroland.nerospace.world.planet.luna;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Locates {@code common/src/main} from the test working directory and reads its JSON. */
final class LunaFiles {

    private LunaFiles() {
    }

    static Path commonMain() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("common/src/main");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** {@code tree} is {@code resources} (26.1.2 / 26.2) or {@code resources-26.3}. */
    static Path data(String tree, String rel) {
        return commonMain().resolve(tree).resolve("data/nerospace").resolve(rel);
    }

    static JsonObject json(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
