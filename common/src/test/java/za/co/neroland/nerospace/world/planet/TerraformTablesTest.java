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
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/** Per-planet terraform tables (D7, 003 §8.1): trio ids, planet rules, data files and lang. */
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

    private static TerraformTable table(String langId) {
        return TerraformTables.forLangId(langId).orElseThrow();
    }
}
