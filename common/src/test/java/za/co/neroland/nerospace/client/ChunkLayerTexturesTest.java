package za.co.neroland.nerospace.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Chunk render layers on 26.x come from the textures, on every loader: a quad whose sprite has any partly
 * transparent pixel (alpha 1-254) is drawn TRANSLUCENT, one with only fully transparent holes CUTOUT, and an opaque
 * one SOLID. There is no per-block layer registration in this mod (see docs/MULTILOADER.md). This test pins the
 * layer each see-through block is meant to get, so a repainted texture cannot silently change it.
 *
 * <p>Add a row for every new glass, ice, crystal or window block.</p>
 */
class ChunkLayerTexturesTest {

    enum Layer { SOLID, CUTOUT, TRANSLUCENT }

    private static final Map<String, Layer> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("crater_glass", Layer.TRANSLUCENT);
        EXPECTED.put("dry_ice", Layer.TRANSLUCENT);
        EXPECTED.put("magma_glass", Layer.TRANSLUCENT);
        EXPECTED.put("frost_crystal_cluster_stage0", Layer.CUTOUT);
        EXPECTED.put("frost_crystal_cluster_stage1", Layer.CUTOUT);
        EXPECTED.put("frost_crystal_cluster_stage2", Layer.CUTOUT);
        EXPECTED.put("frost_crystal_cluster_stage3", Layer.CUTOUT);
        EXPECTED.put("packed_cryo_ice", Layer.SOLID);
        EXPECTED.put("methane_ice", Layer.SOLID);
        EXPECTED.put("permafrost", Layer.SOLID);
        EXPECTED.put("aurora_stone", Layer.SOLID);
        EXPECTED.put("frozen_geyser", Layer.SOLID);
        EXPECTED.put("xertz_soil", Layer.SOLID);
        EXPECTED.put("spore_moss", Layer.SOLID);
        EXPECTED.put("glowcap_block", Layer.SOLID);
        EXPECTED.put("bio_bark_log", Layer.SOLID);
        EXPECTED.put("stripped_bio_bark_log", Layer.SOLID);
        EXPECTED.put("amber_sap_ore", Layer.SOLID);
        EXPECTED.put("glowcap_fungus", Layer.CUTOUT);
        EXPECTED.put("spore_pod", Layer.CUTOUT);
        EXPECTED.put("xertz_vine", Layer.CUTOUT);
        EXPECTED.put("xertz_vine_tip", Layer.CUTOUT);
        EXPECTED.put("shadow_lichen", Layer.CUTOUT);
        EXPECTED.put("nyx_stone", Layer.SOLID);
        EXPECTED.put("dawnband_stone", Layer.SOLID);
        EXPECTED.put("void_crystal", Layer.TRANSLUCENT);
        EXPECTED.put("voidglass_ore", Layer.SOLID);
        EXPECTED.put("gravity_shard_block", Layer.TRANSLUCENT);
        EXPECTED.put("sunglass_crystal", Layer.TRANSLUCENT);
        EXPECTED.put("crazed_crystal", Layer.TRANSLUCENT);
        EXPECTED.put("shard_drift_height2", Layer.SOLID);
        EXPECTED.put("twilight_crystal", Layer.TRANSLUCENT);
        EXPECTED.put("rose_crystal", Layer.TRANSLUCENT);
        EXPECTED.put("rose_crystal_lit", Layer.TRANSLUCENT);
        EXPECTED.put("smoke_crystal", Layer.TRANSLUCENT);
        EXPECTED.put("budding_nyx_crystal", Layer.TRANSLUCENT);
        EXPECTED.put("nyx_crystal_cluster_stage0", Layer.TRANSLUCENT);
        EXPECTED.put("nyx_crystal_cluster_stage1", Layer.TRANSLUCENT);
        EXPECTED.put("nyx_crystal_cluster_stage2", Layer.TRANSLUCENT);
        EXPECTED.put("nyx_crystal_cluster_stage3", Layer.TRANSLUCENT);
        EXPECTED.put("crystal_spire", Layer.TRANSLUCENT);
        EXPECTED.put("crystal_spire_tip", Layer.TRANSLUCENT);
        EXPECTED.put("nyx_prism", Layer.TRANSLUCENT);
        EXPECTED.put("nyx_prism_flaring", Layer.TRANSLUCENT);
        EXPECTED.put("mirror_crystal", Layer.TRANSLUCENT);
        EXPECTED.put("hull_window", Layer.TRANSLUCENT);
        EXPECTED.put("hull_window_frosted", Layer.TRANSLUCENT);
        EXPECTED.put("truss", Layer.CUTOUT);
    }

    private static Path assets() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("common/src/main/resources/assets/nerospace");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Layer layerOf(Path png) throws IOException {
        BufferedImage img = ImageIO.read(png.toFile());
        boolean clear = false;
        boolean partial = false;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int a = img.getRGB(x, y) >>> 24;
                clear |= a == 0;
                partial |= a > 0 && a < 255;
            }
        }
        return partial ? Layer.TRANSLUCENT : clear ? Layer.CUTOUT : Layer.SOLID;
    }

    @Test
    void voidCrystalAnimationHasDistinctTranslucentFrames() throws IOException {
        Path root = assets();
        Assumptions.assumeTrue(root != null, "assets not found");
        Path png = root.resolve("textures/block/void_crystal.png");
        BufferedImage image = ImageIO.read(png.toFile());
        assertEquals(16, image.getWidth());
        assertEquals(128, image.getHeight());
        assertEquals(Layer.TRANSLUCENT, layerOf(png));
        boolean different = false;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                different |= image.getRGB(x, y) != image.getRGB(x, y + 32);
            }
        }
        assertTrue(different, "the pulse must change the texture");
        JsonObject meta = JsonParser.parseString(Files.readString(Path.of(png + ".mcmeta"), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("animation");
        assertTrue(meta.get("interpolate").getAsBoolean());
        assertEquals(12, meta.get("frametime").getAsInt());
    }

    @Test
    @DisplayName("see-through blocks get their intended chunk layer from their textures")
    void layers() throws IOException {
        Path root = assets();
        Assumptions.assumeTrue(root != null, "assets not found");
        for (Map.Entry<String, Layer> e : EXPECTED.entrySet()) {
            Path model = root.resolve("models/block/" + e.getKey() + ".json");
            assertTrue(Files.isRegularFile(model), "missing model " + model);
            JsonObject json = JsonParser.parseString(Files.readString(model, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject textures = json.getAsJsonObject("textures");
            assertTrue(textures != null && !textures.isEmpty(), e.getKey() + " has no textures");
            for (Map.Entry<String, JsonElement> t : textures.entrySet()) {
                String ref = t.getValue().getAsString();
                if (ref.startsWith("#")) {
                    continue;
                }
                String path = ref.substring(ref.indexOf(':') + 1);
                Path png = root.resolve("textures/" + path + ".png");
                assertTrue(Files.isRegularFile(png), "missing texture " + png);
                assertEquals(e.getValue(), layerOf(png), e.getKey() + " texture " + ref);
            }
        }
    }
}
