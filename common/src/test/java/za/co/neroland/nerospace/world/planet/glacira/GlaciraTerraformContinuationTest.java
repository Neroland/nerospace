package za.co.neroland.nerospace.world.planet.glacira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.block.rules.ThawRules;
import za.co.neroland.nerospace.world.planet.TerraformRefusal;
import za.co.neroland.nerospace.world.planet.TerraformTable;
import za.co.neroland.nerospace.world.planet.TreeStyle;

/**
 * [COMPAT] The 1.6.0 fixture's stage-3 Glacira terraformer continues unchanged in 2.0: same radii and energy (the
 * machine's numbers and saved fields are untouched), same stage (the stage-3 Living output is the kept
 * {@code terraformed_tundra} with spruce trees and Woolly Drifts), and no refusal. Permafrost (absent from every
 * 1.6.0 chunk) is the only new rule and thaws at stage 3 only.
 */
class GlaciraTerraformContinuationTest {

    private static String source(String rel) throws IOException {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("common/src/main/java/za/co/neroland/nerospace").resolve(rel);
            if (Files.isRegularFile(c)) {
                return Files.readString(c, StandardCharsets.UTF_8);
            }
        }
        throw new IllegalStateException(rel + " not found");
    }

    @Test
    @DisplayName("Glacira's Living output is the 1.6.0 tundra with its 1.6.0 surface, flora, ores and herd")
    void livingOutputUnchanged() {
        TerraformTable t = GlaciraTerraform.TABLE;
        assertEquals("nerospace:terraformed_tundra", t.living().identifier().toString());
        assertEquals(GlaciraPlanet.INSTANCE.traits().terraformTarget().orElseThrow(), t.living());
        assertEquals("minecraft:grass_block", t.surfaceTop().toString());
        assertEquals("minecraft:dirt", t.surfaceSub().toString());
        assertEquals(List.of(), t.extraSurface());
        assertEquals(Optional.empty(), t.livingTop());
        assertEquals(List.of(), t.livingFlora());
        assertEquals(TerraformTable.legacyFlora(), t.flora());
        assertEquals(TerraformTable.legacyOres(), t.ores());
        assertEquals("nerospace:woolly_drift", t.fauna().get(0).toString());
        assertEquals(Optional.empty(), t.treeFeature());
        assertEquals(1.0, t.treeChance());
        assertEquals(List.of(), t.rules());
        assertEquals(TreeStyle.SPRUCE, GlaciraPlanet.INSTANCE.treeStyle());
        assertEquals(TerraformRefusal.NONE, TerraformRefusal.evaluate(true, false, false));
    }

    @Test
    @DisplayName("permafrost thaws at stage 3 only")
    void thawStage() {
        assertEquals(3, ThawRules.MIN_TERRAFORM_STAGE);
    }

    @Test
    @DisplayName("terraformer energy, buffer and cadence are the 1.6.0 numbers")
    void machineNumbersUnchanged() throws IOException {
        String be = source("machine/TerraformerBlockEntity.java");
        for (String line : List.of("ENERGY_BUFFER = 100_000;", "ENERGY_MAX_INSERT = 2_000;",
                "ENERGY_PER_BLOCK = 12;", "STAGE2_ENERGY_PER_BLOCK = 24;", "STAGE3_ENERGY_PER_BLOCK = 48;",
                "WORK_INTERVAL_TICKS = 8;", "HYDRATION_CAP = 1_024;", "MAX_COLUMNS_PER_TICK = 48;")) {
            assertTrue(be.contains(line), line);
        }
        // The saved stage radii keep their 1.6.0 keys (optional in the codec, so pre-stage saves parse).
        String manager = source("world/TerraformManager.java");
        for (String key : List.of("\"positions\"", "\"radii\"", "\"tiers\"")) {
            assertTrue(manager.contains(key), key);
        }
    }
}
