package za.co.neroland.nerospace.block;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * No two behaviour rows share a (kind, trigger, effect) triple, and every behaviour row has its pure
 * helper under {@code block.rules}. Hand-maintained mirror of the matrix — a new row that duplicates a triple, or a
 * row whose helper is missing, fails here.
 */
class BlockMatrixUniquenessTest {

    record Row(String id, String kind, String trigger, String effect, String helper) {
    }

    static final List<Row> ROWS = List.of(
            // Luna
            new Row("regolith", "state", "step by living", "persistent footprint", "FootprintRules"),
            new Row("lunar_basalt", "solar", "sun + placed", "sinters to polished", "SolarSinterRules"),
            new Row("anorthosite", "solar", "3x3 under ground panel", "+25% reflector", "SolarReflectRules"),
            new Row("crater_glass", "light", "placement/neighbour", "lens +2", "LightLensRules"),
            new Row("helium3_pocket", "yield", "silk or gas tank", "x2 yield", "GasPocketRules"),
            // Mars
            new Row("red_regolith", "ground-change", "dust storm", "moves downwind (conserved)", "DriftRules"),
            new Row("martian_sandstone", "placement", "stonecutter", "4 strata", "StrataRules"),
            new Row("hematite_ore", "yield", "iron tool", "drop magnet", "ItemMagnetRules"),
            new Row("perchlorate_crust", "contact", "step, no sealed boots", "poison", "ContactHazardRules"),
            new Row("dry_ice", "water", "placement/neighbour", "one-shot freeze <= 6", "DryIceRules"),
            new Row("basalt_column", "growth", "one placement", "self-extends to 4", "PillarRules"),
            new Row("dust_devil_pit", "spawner", "dust storm", "spawns dust devil", "SpawnerRules"),
            // Cindara
            new Row("ashstone", "yield", "pickaxe", "instant mine", "SoftStoneRules"),
            new Row("ember_cracked_ashstone", "contact", "step, no heat boots", "fire damage", "HeatContactRules"),
            new Row("ember_cracked_ashstone#quench", "water", "adjacent water", "quenches to ashstone", "QuenchRules"),
            new Row("sulfur_crust", "fire", "adjacent fire", "ignites <= 5 neighbours", "IgniteRules"),
            new Row("magma_glass", "fire", "lava contact", "immune", "LavaImmuneRules"),
            new Row("ash_layer", "ground-change", "ash fall", "accumulates layers (cap 4)", "LayerRules"),
            new Row("basalt_spire", "fire", "ember strike", "lightning cancelled", "LightningRules"),
            // Glacira
            new Row("packed_cryo_ice", "water", "passive", "friction 0.989, never melts", "CryoIceRules"),
            new Row("frost_crystal_cluster", "growth", "diamond dust end", "stages in place", "CrystalStageRules"),
            new Row("methane_ice", "fire", "flame within 2", "explodes (chain cap)", "MethaneRules"),
            new Row("permafrost", "water", "hydration/terraform", "hydration-immune", "ThawRules"),
            new Row("aurora_stone", "light", "clock night", "emits 8", "AuroraRules"),
            new Row("frozen_geyser", "gravity", "timer", "impulse +1.2", "GeyserRules"),
            // Greenxertz
            new Row("xertz_soil", "crop-gate", "rootable tag", "only Xertz substrate", "RootRules"),
            new Row("spore_moss", "growth", "random tick on soil", "spreads, cap 48, x3 bloom", "SporeSpreadRules"),
            new Row("spore_moss#contact", "contact", "step, no sealed helmet", "point nausea", "SporeContactRules"),
            new Row("glowcap_fungus", "growth", "bone meal", "giant glowcap", "GrowthRules"),
            new Row("stripped_bio_bark_log", "rest", "wake within 4", "+4 HP regen", "HealBedRules"),
            new Row("xertz_vine", "growth", "random tick", "grows down to 12", "VineRules"),
            new Row("spore_pod", "contact", "collision/punch", "area nausea, consumed", "SporeBurstRules"),
            // Nyxara
            new Row("nyx_stone", "light", "propagation", "absorbs 2", "LightAbsorbRules"),
            new Row("void_crystal", "light", "beam/sightline", "refracts 90 deg", "RefractRules"),
            new Row("dawnband_stone", "light", "flare", "emits 11 + warms", "WarmthRules"),
            new Row("shadow_lichen", "growth", "random tick, light <= 3", "spreads faces, cap 32, withers", "LichenRules"),
            new Row("gravity_shard_block", "gravity", "presence", "-0.15 in 3x5x3", "BlockGravityRules"),
            // Station
            new Row("hull_plating", "airtight", "tag", "airtight-counted opaque", "AirtightRules"),
            new Row("hull_window", "airtight", "tag + void", "transparent seal, frost", "FrostRules"),
            new Row("truss", "placement", "sneak-place", "run of 8", "RunPlacementRules"),
            new Row("solar_wing", "solar", "attached, void-exposed", "2x T2 generator", "SolarWingRules"),
            new Row("docking_ring", "return-site", "rocket arrival", "DOCK node 3x3", "DockRules"),
            new Row("gravity_plate", "gravity", "presence", "1.0 in 1x6x1, beats shard", "BlockGravityRules"),
            new Row("hydroponics_tray", "crop-gate", "window above + oxygen", "crops grow in void", "HydroRules"),
            new Row("telescope", "reveal", "sightline", "reveals 1 + min(2, crystals)", "TelescopeSightline"),
            new Row("cheese_wheel", "decor", "slice", "9 in, 9 out", "CheeseRules"));

    /** Helpers whose cases live in a shared test class (see each class's display names). */
    static final Map<String, String> TESTS = Map.ofEntries(
            Map.entry("HeatContactRules", "ContactHazardRulesTest"),
            Map.entry("SporeContactRules", "ContactHazardRulesTest"),
            Map.entry("SporeSpreadRules", "SpreadRulesTest"),
            Map.entry("VineRules", "SpreadRulesTest"),
            Map.entry("LichenRules", "SpreadRulesTest"),
            Map.entry("FrostRules", "StationRulesTest"),
            Map.entry("RunPlacementRules", "StationRulesTest"),
            Map.entry("SolarWingRules", "StationRulesTest"),
            Map.entry("DockRules", "StationRulesTest"),
            Map.entry("HydroRules", "StationRulesTest"));

    @Test
    @DisplayName("no two rows share (kind, trigger, effect)")
    void unique() {
        Map<String, String> seen = new HashMap<>();
        for (Row r : ROWS) {
            String key = r.kind() + "|" + r.trigger() + "|" + r.effect();
            String prev = seen.put(key, r.id());
            assertTrue(prev == null, r.id() + " duplicates " + prev);
        }
        assertEquals(ROWS.size(), seen.size());
    }

    @Test
    @DisplayName("every behaviour row has its pure helper and a JUnit test next to it")
    void helpersExist() {
        for (Row r : ROWS) {
            String cls = "za.co.neroland.nerospace.block.rules." + r.helper();
            assertDoesNotThrow(() -> Class.forName(cls), r.id() + " helper " + cls);
            String test = TESTS.getOrDefault(r.helper(), r.helper() + "Test");
            assertDoesNotThrow(() -> Class.forName("za.co.neroland.nerospace.block.rules." + test),
                    r.id() + " test " + test);
        }
    }
}
