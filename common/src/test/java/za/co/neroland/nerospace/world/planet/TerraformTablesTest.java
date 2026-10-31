package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/** Per-planet terraform tables: trio ids, planet rules, data files and lang. */
class TerraformTablesTest {

    private static Path commonMain() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("common/src/main");
            if (Files.isDirectory(c)) {
                return c;
            }
        }
        throw new IllegalStateException("common/src/main not found");
    }

    private static JsonObject lang() throws IOException {
        return JsonParser.parseString(Files.readString(
                commonMain().resolve("resources/assets/nerospace/lang/en_us.json"), StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    private static String path(ResourceKey<Biome> key) {
        return key.identifier().getPath();
    }

    @Test
    @DisplayName("every planet but the station has a table whose Living biome is its terraformTarget")
    void tablePerPlanet() {
        for (PlanetDefinition p : Planets.ALL) {
            if (p == Planets.STATION) {
                assertTrue(p.terraform().isEmpty(), "the station has nothing to terraform");
                continue;
            }
            TerraformTable t = p.terraform().orElseThrow();
            assertEquals(p.langId(), t.langId());
            assertEquals(p.traits().terraformTarget().orElseThrow(), t.living(), p.langId());
        }
    }

    @Test
    @DisplayName("trios follow terraformed_<planet>_rooted/hydrated/living; kept ids stay the stage-3 ids")
    void trioNaming() {
        for (TerraformTable t : TerraformTables.ORDER) {
            if (t == TerraformTables.LEGACY) {
                continue;
            }
            assertEquals("terraformed_" + t.langId() + "_rooted", path(t.rooted()));
            assertEquals("terraformed_" + t.langId() + "_hydrated", path(t.hydrated()));
            String living = path(t.living());
            switch (t.langId()) {
                case "cindara" -> assertEquals("terraformed_savanna", living);
                case "glacira" -> assertEquals("terraformed_tundra", living);
                default -> assertEquals("terraformed_" + t.langId() + "_living", living);
            }
        }
        assertEquals("terraformed", path(TerraformTables.LEGACY.rooted()));
        assertEquals("terraformed_meadow", path(TerraformTables.LEGACY.living()));
    }

    @Test
    @DisplayName("every trio biome (and every kept 1.x id) ships as a biome file")
    void biomeFilesExist() {
        Path dir = commonMain().resolve("resources/data/nerospace/worldgen/biome");
        Set<String> ids = new HashSet<>(List.of("terraformed", "terraformed_meadow", "terraformed_savanna",
                "terraformed_tundra"));
        for (TerraformTable t : TerraformTables.ORDER) {
            ids.add(path(t.rooted()));
            ids.add(path(t.hydrated()));
            ids.add(path(t.living()));
        }
        for (String id : ids) {
            assertTrue(Files.isRegularFile(dir.resolve(id + ".json")), id);
        }
    }

    @Test
    @DisplayName("planet rules: Nyxara band only, Luna shimmer dome, Mars open water, Greenxertz xertz soil")
    void planetRules() {
        assertEquals(List.of(TerraformTable.Rule.BAND_ONLY), table("nyxara").rules());
        assertEquals(List.of(TerraformTable.Rule.SHIMMER_DOME), table("luna").rules());
        assertEquals(List.of(TerraformTable.Rule.OPEN_WATER_SPRING), table("mars").rules());
        assertEquals("nerospace:xertz_soil", table("greenxertz").livingTop().orElseThrow().toString());
        assertEquals(0.0, table("mars").treeChance(), "the red steppe is treeless");
        for (TerraformTable t : TerraformTables.ORDER) {
            assertEquals("minecraft:water", t.liquid().toString(), t.langId());
            assertFalse(t.ores().isEmpty(), t.langId());
            assertFalse(t.flora().isEmpty(), t.langId());
            assertFalse(t.ores().toString().contains("cheese"), "terraforming never seeds cheese");
            if (!t.has(TerraformTable.Rule.SHIMMER_DOME)) {
                assertFalse(t.langId().equals("luna"));
            }
        }
    }

    @Test
    @DisplayName("refusal: no table → no surface; band-only outside the band → out of band")
    void refusal() {
        assertEquals(TerraformRefusal.NO_SURFACE, TerraformRefusal.evaluate(false, false, true));
        assertEquals(TerraformRefusal.OUT_OF_BAND, TerraformRefusal.evaluate(true, true, false));
        assertEquals(TerraformRefusal.NONE, TerraformRefusal.evaluate(true, true, true));
        assertEquals(TerraformRefusal.NONE, TerraformRefusal.evaluate(true, false, false));
        assertEquals(TerraformRefusal.NONE, TerraformRefusal.byOrdinal(99));
    }

    @Test
    @DisplayName("the terraformed grove keeps the band's radiation")
    void groveRadiation() {
        for (String s : List.of("rooted", "hydrated", "living")) {
            assertEquals(1, RadiationRules.base(3, "nerospace:terraformed_nyxara_" + s));
        }
        assertEquals(3, RadiationRules.base(3, "nerospace:nyxara_nightside"));
    }

    @Test
    @DisplayName("Monitor readout and refusal reasons are translated for every table")
    void langKeys() throws IOException {
        JsonObject lang = lang();
        for (TerraformTable t : TerraformTables.ORDER) {
            for (String s : List.of("1", "2", "3", "done")) {
                String key = "gui.nerospace.terraform_monitor.next." + t.langId() + "." + s;
                assertTrue(lang.has(key), key);
            }
        }
        for (TerraformRefusal r : TerraformRefusal.values()) {
            if (r != TerraformRefusal.NONE) {
                assertTrue(lang.has(r.langKey()), r.langKey());
            }
        }
    }

    @Test
    @DisplayName("weighted picks cover the whole weight range")
    void weightedPick() {
        List<TerraformTable.Weighted> flora = TerraformTable.legacyFlora();
        assertEquals(100, TerraformTable.totalWeight(flora));
        assertEquals("minecraft:oak_sapling", TerraformTable.pick(flora, 0).orElseThrow().toString());
        assertEquals("minecraft:short_grass", TerraformTable.pick(flora, 99).orElseThrow().toString());
        assertTrue(TerraformTable.pick(flora, 100).isEmpty());
    }

    /** Vanilla {@code #minecraft:dirt}, which {@code #nerospace:terraform_to_grass} includes by reference. */
    private static final Set<String> VANILLA_DIRT = Set.of("minecraft:dirt", "minecraft:grass_block",
            "minecraft:podzol", "minecraft:coarse_dirt", "minecraft:mycelium", "minecraft:rooted_dirt",
            "minecraft:moss_block", "minecraft:mud", "minecraft:muddy_mangrove_roots", "minecraft:pale_moss_block");

    /**
     * Natural surface blocks that deliberately stay through stages 1-2 (and are therefore neither tagged nor listed).
     * Every entry needs a reason; anything else on a planet's surface must convert.
     */
    private static final Map<String, Set<String>> HOLDS = Map.of(
            // Permafrost thaws at Living only (hydration_immune); Packed Cryo-Ice is glacier ice and stays ice.
            "glacira", Set.of("nerospace:permafrost", "nerospace:packed_cryo_ice", "minecraft:packed_ice"),
            // The polar caps stay dry ice.
            "mars", Set.of("nerospace:dry_ice"));

    /** Blocks a surface rule (26.3: material rule) writes on the top floor block (a {@code stone_depth} floor condition at offset 0). */
    private static void surfaceBlocks(com.google.gson.JsonElement node, boolean floor, Set<String> out) {
        if (node == null || !node.isJsonObject()) {
            return;
        }
        JsonObject o = node.getAsJsonObject();
        String type = o.has("type") ? o.get("type").getAsString() : "";
        switch (type) {
            case "minecraft:block" -> {
                if (floor) {
                    // 26.1.2 / 26.2: {"Name": id, ...}; 26.3: a plain state string, or {"id": id, "properties": ...}.
                    com.google.gson.JsonElement state = o.get("result_state");
                    out.add(state.isJsonObject()
                            ? state.getAsJsonObject().get(state.getAsJsonObject().has("Name") ? "Name" : "id").getAsString()
                            : state.getAsString().replaceFirst("\\[.*$", ""));
                }
            }
            case "minecraft:condition" -> {
                JsonObject cond = o.getAsJsonObject("if_true");
                boolean top = "minecraft:stone_depth".equals(cond.get("type").getAsString())
                        && "floor".equals(cond.get("surface_type").getAsString())
                        && (!cond.has("offset") || cond.get("offset").getAsInt() == 0);
                surfaceBlocks(o.get("then_run"), floor || top, out);
            }
            case "minecraft:sequence" -> {
                for (com.google.gson.JsonElement e : o.getAsJsonArray("sequence")) {
                    surfaceBlocks(e, floor, out);
                }
            }
            default -> {
            }
        }
    }

    private static Set<String> tagValues(String tag) throws IOException {
        Set<String> out = new HashSet<>();
        JsonObject json = JsonParser.parseString(Files.readString(
                commonMain().resolve("resources/data/nerospace/tags/block/" + tag + ".json"), StandardCharsets.UTF_8))
                .getAsJsonObject();
        for (com.google.gson.JsonElement e : json.getAsJsonArray("values")) {
            String id = e.isJsonObject() ? e.getAsJsonObject().get("id").getAsString() : e.getAsString();
            if ("#minecraft:dirt".equals(id)) {
                out.addAll(VANILLA_DIRT);
            } else {
                out.add(id);
            }
        }
        return out;
    }

    @Test
    @DisplayName("every block a planet's noise settings put on the surface converts at stage 1 (tagged or listed)")
    void surfaceBlocksAreTerraformable() throws IOException {
        Set<String> tagged = tagValues("terraform_to_grass");
        for (TerraformTable t : TerraformTables.ORDER) {
            if (t == TerraformTables.LEGACY) {
                continue;
            }
            Set<String> listed = new HashSet<>(tagged);
            t.extraSurface().forEach(id -> listed.add(id.toString()));
            listed.addAll(HOLDS.getOrDefault(t.langId(), Set.of()));
            int checked = 0;
            for (String tree : List.of("resources", "resources-26.3")) {
                Path file = commonMain().resolve(tree + "/data/nerospace/worldgen/noise_settings/" + t.langId() + ".json");
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                Set<String> surface = new HashSet<>();
                JsonObject settings = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                surfaceBlocks(settings.has("surface_rule") ? settings.get("surface_rule")
                        : settings.get("material_rule"), false, surface);
                assertFalse(surface.isEmpty(), tree + " " + t.langId() + ": no surface blocks found");
                for (String block : surface) {
                    assertTrue(listed.contains(block), tree + " " + t.langId() + ": surface block " + block
                            + " is neither in #nerospace:terraform_to_grass nor in the table's extraSurface");
                }
                checked++;
            }
            assertTrue(checked > 0, t.langId() + ": no noise settings");
        }
        assertTrue(table("greenxertz").extraSurface().stream().anyMatch(id -> id.toString().equals("nerospace:xertz_soil")),
                "the xertz garden must take on 2.0 Greenxertz ground");
    }

    private static TerraformTable table(String langId) {
        return TerraformTables.forLangId(langId).orElseThrow();
    }
}
