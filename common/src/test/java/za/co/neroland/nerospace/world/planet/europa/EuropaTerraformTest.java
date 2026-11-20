package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.world.TerraformFaunaRules;
import za.co.neroland.nerospace.world.planet.FreezeGuardRules;
import za.co.neroland.nerospace.world.planet.OpenOceanRules;
import za.co.neroland.nerospace.world.planet.TerraformTable;
import za.co.neroland.nerospace.world.planet.TerraformTable.Rule;
import za.co.neroland.nerospace.world.planet.TerraformTables;

/**
 * Europa's terraform table (design 10.1 / 10.4, amendments F03, F09, F10, D24): an ocean opens at stage 3, the
 * world never becomes breathable, the open sea of the trio does not freeze, and the herd is a water herd.
 */
class EuropaTerraformTest {

    private static final TerraformTable T = EuropaTerraform.TABLE;

    @Test
    @DisplayName("the table resolves by langId at ORDER index 7, with the trio, no trees and no ground cover")
    void table() {
        assertEquals("europa", T.langId());
        assertEquals(T, TerraformTables.forLangId("europa").orElseThrow());
        assertEquals(7, TerraformTables.indexOf(T));
        assertEquals(T, TerraformTables.byIndex(7));
        assertEquals("nerospace:terraformed_europa_rooted", T.rooted().identifier().toString());
        assertEquals("nerospace:terraformed_europa_hydrated", T.hydrated().identifier().toString());
        assertEquals("nerospace:terraformed_europa_living", T.living().identifier().toString());
        assertEquals(0.0D, T.treeChance());
        assertTrue(T.treeFeature().isEmpty());
        assertTrue(T.flora().isEmpty(), "F10: no snow layers");
        assertTrue(T.livingFlora().isEmpty());
        assertTrue(T.livingTop().isEmpty(), "OPEN_OCEAN writes the stage-3 surface");
        assertEquals("minecraft:water", T.liquid().toString());
        assertEquals(List.of(Identifier.fromNamespaceAndPath("nerospace", "lantern_drifter")), T.fauna());
    }

    @Test
    @DisplayName("rules: a spring, an open ocean and no atmosphere, in that order")
    void rules() {
        assertEquals(List.of(Rule.OPEN_WATER_SPRING, Rule.OPEN_OCEAN, Rule.NO_ATMOSPHERE), T.rules());
        assertTrue(T.has(Rule.OPEN_OCEAN));
        assertTrue(T.has(Rule.NO_ATMOSPHERE));
        // No other table opens an ocean or keeps its world airless, so their conversion and breathing are unchanged.
        for (TerraformTable other : TerraformTables.ORDER) {
            if (other != T) {
                assertFalse(other.has(Rule.OPEN_OCEAN), other.langId());
                assertFalse(other.has(Rule.NO_ATMOSPHERE), other.langId());
            }
        }
    }

    @Test
    @DisplayName("every Nerospace id in the table is a Europa block or the registered drifter")
    void idsResolve() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        List<Identifier> blocks = new ArrayList<>();
        blocks.add(T.surfaceTop());
        blocks.add(T.surfaceSub());
        blocks.addAll(T.extraSurface());
        blocks.addAll(T.ores());
        for (Identifier id : blocks) {
            if (id.getNamespace().equals("nerospace")) {
                assertTrue(EuropaFiles.BLOCKS.contains(id.getPath()), id.toString());
            } else {
                assertEquals("minecraft", id.getNamespace(), id.toString());
            }
        }
        String content = Files.readString(EuropaFiles.commonMain().resolve(
                "java/za/co/neroland/nerospace/world/planet/europa/EuropaContent.java"), StandardCharsets.UTF_8);
        assertTrue(content.contains("\"lantern_drifter\""));
        String planet = Files.readString(EuropaFiles.commonMain().resolve(
                "java/za/co/neroland/nerospace/world/planet/europa/EuropaPlanet.java"), StandardCharsets.UTF_8);
        assertTrue(planet.contains("EuropaContent.LANTERN_DRIFTER.get()"), "livestock is the drifter");
    }

    @Test
    @DisplayName("the sea stays open: hydrated/living 0.2 (no freeze), rooted 0.0, no precipitation, both trees")
    void seaStaysOpen() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        Map<String, Double> temps = Map.of("terraformed_europa_rooted", 0.0D, "terraformed_europa_hydrated", 0.2D,
                "terraformed_europa_living", 0.2D);
        for (String tree : EuropaFiles.TREES) {
            for (Map.Entry<String, Double> e : temps.entrySet()) {
                JsonObject b = EuropaFiles.json(EuropaFiles.data(tree, "worldgen/biome/" + e.getKey() + ".json"));
                float t = b.get("temperature").getAsFloat();
                assertEquals(e.getValue(), t, 1e-6, tree + " " + e.getKey());
                assertFalse(b.get("has_precipitation").getAsBoolean(), tree + " " + e.getKey());
                assertEquals(e.getKey().equals("terraformed_europa_rooted"), FreezeGuardRules.biomeFreezes(t),
                        tree + " " + e.getKey());
            }
        }
        // Natural Europa water does skin over.
        assertTrue(FreezeGuardRules.biomeFreezes(0.0F));
    }

    @Test
    @DisplayName("stage 3 opens at most four cells under blades and snow, and the herd seeds only in that water")
    void openOceanAndHerd() {
        assertEquals(4, OpenOceanRules.MAX_CELLS);
        assertEquals(2, OpenOceanRules.topIndex(List.of("nerospace:penitente_blade", "minecraft:snow",
                "minecraft:snow_block", "nerospace:europa_ice")));
        assertEquals(4, OpenOceanRules.cells(true, 3));
        assertEquals(0, OpenOceanRules.cells(false, 3), "a player build on top is never eaten");
        assertTrue(TerraformFaunaRules.maySeed(T.has(Rule.OPEN_OCEAN), true));
        assertFalse(TerraformFaunaRules.maySeed(T.has(Rule.OPEN_OCEAN), false));
    }

    @Test
    @DisplayName("the herd guard and the stage-3 order are wired: OPEN_OCEAN before seedHerd, guard in TerraformFauna")
    void wired() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        String conv = Files.readString(EuropaFiles.commonMain().resolve(
                "java/za/co/neroland/nerospace/machine/TerraformConversion.java"), StandardCharsets.UTF_8);
        int open = conv.indexOf("openOcean(level, table, x, surfaceY, z)");
        int seed = conv.indexOf("TerraformFauna.seedHerd(level, table, x, herdY, z)");
        assertTrue(open > 0 && seed > open, "OPEN_OCEAN runs before the herd");
        String fauna = Files.readString(EuropaFiles.commonMain().resolve(
                "java/za/co/neroland/nerospace/world/TerraformFauna.java"), StandardCharsets.UTF_8);
        assertTrue(fauna.contains("TerraformFaunaRules.maySeed("));
    }
}
