package za.co.neroland.nerospace.world.planet.europa;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Locates {@code common/src/main} from the test working directory and reads its JSON. */
final class EuropaFiles {

    /** The two resource trees: 26.1.2 / 26.2 (and, rewritten at build time, 1.21.x), and the 26.3 overlay. */
    static final List<String> TREES = List.of("resources", "resources-26.3");

    static final List<String> SURFACE_BIOMES = List.of("europa_ice_plains", "europa_chaos", "europa_lineae",
            "europa_penitentes");
    static final List<String> NATURAL_BIOMES = List.of("europa_ice_plains", "europa_chaos", "europa_lineae",
            "europa_penitentes", "europa_shell", "europa_abyss", "europa_vent_fields");
    static final List<String> TRIO = List.of("terraformed_europa_rooted", "terraformed_europa_hydrated",
            "terraformed_europa_living");

    /** Every Europa block id, in registration order. */
    static final List<String> BLOCKS = List.of("europa_ice", "chaos_ice", "cut_europa_ice", "europa_ice_bricks",
            "lineae_salt", "penitente_blade", "shell_ice", "cryovent", "hydrothermal_vent", "vent_basalt",
            "polished_vent_basalt", "vent_basalt_bricks", "tube_mat", "smoker_chimney", "deuterium_ice", "oxidant_ice",
            "brine_salt_ore", "vent_sulfide_ore", "europium_ore", "europium_lamp", "brine");
    /** Every Europa item that is not a block item. */
    static final List<String> ITEMS = List.of("deuterium_shard", "deuterium_cell", "oxidant_crystal", "brine_salt",
            "raw_vent_sulfide", "raw_europium", "brine_bucket");

    private EuropaFiles() {
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

    static Path assets(String rel) {
        return commonMain().resolve("resources/assets/nerospace").resolve(rel);
    }

    static JsonObject json(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
