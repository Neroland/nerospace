package za.co.neroland.nerospace.client.sky;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The baked body rotation of 2.4.0 ({@link SkyProfile.Rotation}, Europa's drifting Jupiter): the field is optional and
 * every profile without it decodes and draws exactly as before, the frame selection and cross-fade are the pure maths
 * {@link SkyScene} uses, and every frame a profile names exists in both body styles.
 */
class SkyRotationTest {

    /** The profiles that shipped before the rotation field (Europa is checked with its rotation removed). */
    private static final List<String> EXISTING = List.of("luna", "mars", "cindara", "glacira", "greenxertz", "nyxara",
            "station", "europa");
    private static final Identifier JUPITER_STILL = Identifier.fromNamespaceAndPath("nerospace",
            "textures/sky/jupiter.png");
    private static final Identifier JUPITER_SHEET = Identifier.fromNamespaceAndPath("nerospace",
            "textures/sky/jupiter");

    private static Path repo() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("common/src/main/resources/assets/nerospace"))) {
                return p;
            }
        }
        return null;
    }

    private static Path assets() {
        Path r = repo();
        Assumptions.assumeTrue(r != null, "repo not found");
        return r.resolve("common/src/main/resources/assets/nerospace");
    }

    private static JsonObject read(String sky) throws IOException {
        return JsonParser.parseString(Files.readString(assets().resolve("sky/" + sky + ".json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static SkyProfile decode(JsonElement json) {
        return SkyProfile.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
    }

    /** The profile JSON with every body's {@code rotation} object removed (the pre-2.4.0 shape). */
    private static JsonObject withoutRotation(JsonObject json) {
        JsonObject copy = json.deepCopy();
        for (String list : new String[] {"sun", "moons", "bodies"}) {
            if (copy.has(list)) {
                JsonArray a = copy.getAsJsonArray(list);
                for (JsonElement e : a) {
                    e.getAsJsonObject().remove("rotation");
                }
            }
        }
        return copy;
    }

    private static List<SkyProfile.Body> bodies(SkyProfile p) {
        List<SkyProfile.Body> all = new ArrayList<>(p.sun());
        all.addAll(p.moons());
        all.addAll(p.bodies());
        return all;
    }

    private static SkyScene scene(SkyProfile p, double timeTicks, SkyScene.Config config) {
        double phase = 0.6;
        return SkyScene.compute(p, new SkyScene.Inputs(phase, 3.0 + phase, 0.0, "NONE", 0.0F, "NONE", 0.0F, 0.0, 2048,
                timeTicks, 24_000L, true, 7L, config));
    }

    private static List<String> quadTextures(SkyScene s) {
        List<String> out = new ArrayList<>();
        for (SkyScene.Quad q : s.quads) {
            out.add(q.texture() + "@" + q.alpha());
        }
        return out;
    }

    @Test
    @DisplayName("a profile without rotation decodes and draws as before (all eight shipped profiles)")
    void absentIsUnchanged() throws IOException {
        for (String sky : EXISTING) {
            JsonObject json = read(sky);
            SkyProfile stripped = decode(withoutRotation(json));
            for (SkyProfile.Body b : bodies(stripped)) {
                assertTrue(b.rotation().isEmpty(), sky + "/" + b.id());
            }
            // The 15-field body constructor (the 2.3 shape) builds the same body.
            for (SkyProfile.Body b : bodies(stripped)) {
                assertEquals(b, new SkyProfile.Body(b.id(), b.texture(), b.size(), b.color(), b.glow(),
                        b.orbitPeriodDays(), b.phaseOffset(), b.inclination(), b.fixedAngle(), b.phases(),
                        b.glowTexture(), b.limbGlow(), b.horizonGlow(), b.nightShade(), b.earthshine()), sky);
            }
            // Re-encoding writes no rotation key, and the scene draws no frame texture at any time.
            JsonObject encoded = SkyProfile.CODEC.encodeStart(JsonOps.INSTANCE, stripped).result().orElseThrow()
                    .getAsJsonObject();
            assertFalse(encoded.toString().contains("\"rotation\""), sky);
            for (double t : new double[] {0.0, 1234.5, 23_999.0, 100_000.0}) {
                for (String q : quadTextures(scene(stripped, t, SkyScene.Config.FULL))) {
                    assertFalse(q.matches(".*_\\d\\d\\.png@.*") && !q.contains("terminator_"), sky + " " + q);
                }
            }
            if (!"europa".equals(sky)) {
                // Profiles other than Europa have no rotation at all: the shipped file is the stripped one.
                assertEquals(stripped, decode(json), sky);
            }
        }
        // Europa without its rotation object draws the still Jupiter in full and lite quality alike.
        SkyProfile still = decode(withoutRotation(read("europa")));
        assertEquals(quadTextures(scene(still, 5000.0, SkyScene.Config.of(true, true, true))).stream()
                        .filter(q -> q.startsWith(JUPITER_STILL.toString())).count(),
                quadTextures(scene(still, 5000.0, SkyScene.Config.FULL)).stream()
                        .filter(q -> q.startsWith(JUPITER_STILL.toString())).count());
        assertTrue(quadTextures(scene(still, 5000.0, SkyScene.Config.FULL)).contains(JUPITER_STILL + "@1.0"));
    }

    @Test
    @DisplayName("Europa's Jupiter carries the 32-frame drift; omitted fields take their defaults")
    void presentIsDecoded() throws IOException {
        SkyProfile europa = decode(read("europa"));
        SkyProfile.Body jupiter = europa.bodies().stream().filter(b -> b.id().equals("jupiter")).findFirst()
                .orElseThrow();
        assertEquals(Optional.of(JUPITER_STILL), jupiter.texture(), "the still stays the fallback and lite frame");
        assertEquals(new SkyProfile.Rotation(JUPITER_SHEET, 32, 24_000), jupiter.rotation().orElseThrow());
        for (SkyProfile.Body b : bodies(europa)) {
            assertEquals("jupiter".equals(b.id()), b.rotation().isPresent(), b.id());
        }
        SkyProfile.Body minimal = decode(JsonParser.parseString("{\"bodies\": [{\"id\": \"g\", \"texture\": "
                + "\"nerospace:textures/sky/jupiter.png\", \"rotation\": {\"texture\": \"nerospace:textures/sky/jupiter\""
                + "}}]}")).bodies().get(0);
        SkyProfile.Rotation r = minimal.rotation().orElseThrow();
        assertEquals(SkyProfile.Rotation.DEFAULT_FRAMES, r.frames());
        assertEquals(1, r.frames());
        assertEquals(24_000, r.periodTicks());
        assertFalse(SkyScene.rotates(minimal, false), "one frame is the still");
        // Round trip keeps the rotation.
        JsonElement json = SkyProfile.CODEC.encodeStart(JsonOps.INSTANCE, europa).result().orElseThrow();
        assertEquals(europa, decode(json));
    }

    @Test
    @DisplayName("frame selection: f = frac(t / period) * frames; floor(f) opaque, the next at frac(f), wrapping")
    void frameMaths() {
        assertEquals(0.0, SkyScene.rotationPosition(0.0, 32, 24_000), 1e-9);
        assertEquals(16.0, SkyScene.rotationPosition(12_000.0, 32, 24_000), 1e-9);
        assertEquals(0.0, SkyScene.rotationPosition(24_000.0, 32, 24_000), 1e-9);
        assertEquals(3.25, SkyScene.rotationPosition(24_000.0 * 7 + 750.0 * 3.25, 32, 24_000), 1e-9);
        // Negative game time still lands inside the loop.
        double neg = SkyScene.rotationPosition(-375.0, 32, 24_000);
        assertEquals(31.5, neg, 1e-9);
        // Degenerate rotations sit on frame 0.
        assertEquals(0.0, SkyScene.rotationPosition(5000.0, 1, 24_000), 1e-9);
        assertEquals(0.0, SkyScene.rotationPosition(5000.0, 32, 0), 1e-9);

        assertEquals(3, SkyScene.rotationFrameA(3.25, 32));
        assertEquals(4, SkyScene.rotationFrameB(3.25, 32));
        assertEquals(0.25F, SkyScene.rotationBlend(3.25), 1e-6F);
        assertEquals(31, SkyScene.rotationFrameA(31.9, 32));
        assertEquals(0, SkyScene.rotationFrameB(31.9, 32), "the loop closes on frame 0");
        assertEquals(0.0F, SkyScene.rotationBlend(5.0), 1e-6F);
        assertEquals(31, SkyScene.rotationFrameA(99.0, 32), "clamped");

        // Continuity: the drawn mix never jumps between neighbouring ticks.
        double prev = mix(0.0);
        for (int t = 1; t <= 24_000; t++) {
            double m = mix(t);
            double step = Math.abs(m - prev);
            assertTrue(Math.min(step, 32.0 - step) < 0.01, "jump at tick " + t);
            prev = m;
        }
    }

    // The effective frame position the two quads show: frame A plus the blend toward B.
    private static double mix(double t) {
        double f = SkyScene.rotationPosition(t, 32, 24_000);
        return SkyScene.rotationFrameA(f, 32) + SkyScene.rotationBlend(f);
    }

    @Test
    @DisplayName("the scene cross-fades two frames in full quality and draws the still in lite quality")
    void sceneDrawsFrames() throws IOException {
        SkyProfile europa = decode(read("europa"));
        double t = 750.0 * 3.25;   // f = 3.25
        List<String> full = quadTextures(scene(europa, t, SkyScene.Config.FULL));
        assertTrue(full.contains("nerospace:textures/sky/jupiter_03.png@1.0"), full.toString());
        assertTrue(full.contains("nerospace:textures/sky/jupiter_04.png@0.25"), full.toString());
        assertFalse(full.stream().anyMatch(q -> q.startsWith(JUPITER_STILL.toString())), "no still in full quality");
        int framesFull = (int) full.stream().filter(q -> q.contains("textures/sky/jupiter_")).count();
        assertEquals(2, framesFull);
        // On an exact frame only one quad is drawn.
        List<String> exact = quadTextures(scene(europa, 750.0 * 5, SkyScene.Config.FULL));
        assertEquals(1, exact.stream().filter(q -> q.contains("textures/sky/jupiter_")).count());
        assertTrue(exact.contains("nerospace:textures/sky/jupiter_05.png@1.0"), exact.toString());
        // Lite draws the still only.
        List<String> lite = quadTextures(scene(europa, t, SkyScene.Config.of(true, true, true)));
        assertTrue(lite.contains(JUPITER_STILL + "@1.0"), lite.toString());
        assertFalse(lite.stream().anyMatch(q -> q.contains("textures/sky/jupiter_")), lite.toString());
        // Everything else in the scene is the same as the still profile's.
        SkyProfile still = decode(withoutRotation(read("europa")));
        List<String> stillFull = quadTextures(scene(still, t, SkyScene.Config.FULL));
        List<String> others = new ArrayList<>(full);
        others.removeIf(q -> q.contains("textures/sky/jupiter_"));
        List<String> stillOthers = new ArrayList<>(stillFull);
        stillOthers.removeIf(q -> q.startsWith(JUPITER_STILL + "@"));
        assertEquals(stillOthers, others);
    }

    @Test
    @DisplayName("every rotation frame exists as <base>_NN.png, with a square-style variant")
    void framesExist() throws IOException {
        Path a = assets();
        for (String sky : EXISTING) {
            for (SkyProfile.Body b : bodies(decode(read(sky)))) {
                if (b.rotation().isEmpty()) {
                    continue;
                }
                SkyProfile.Rotation r = b.rotation().get();
                assertTrue(b.texture().isPresent(), sky + "/" + b.id() + " needs a still for lite quality");
                for (int i = 0; i < r.frames(); i++) {
                    Identifier frame = SkyScene.rotationTexture(r.texture(), i, r.frames());
                    assertEquals(SkyScene.frameTexture(r.texture(), i), frame);
                    assertEquals(r.texture().getPath() + String.format("_%02d.png", i), frame.getPath());
                    assertTrue(Files.isRegularFile(a.resolve(frame.getPath())), "missing " + frame);
                    // The square body style swaps each frame for its square variant, like the still.
                    String square = SkyBodyStyle.squarePath(frame.getPath());
                    assertTrue(square.startsWith(SkyBodyStyle.SQUARE_FOLDER), square);
                    assertTrue(Files.isRegularFile(a.resolve(square)), "missing square variant " + square);
                    assertEquals(square, SkyBodyStyle.select(frame.getPath(), SkyBodyStyle.SQUARE,
                            p -> Files.isRegularFile(a.resolve(p))));
                }
                assertTrue(Files.isRegularFile(a.resolve(SkyBodyStyle.squarePath(b.texture().get().getPath()))));
            }
        }
        // Three-digit frames keep the plain number.
        assertTrue(SkyScene.frameTexture(JUPITER_SHEET, 100).getPath().endsWith("jupiter_100.png"));
        assertTrue(SkyScene.frameTexture(JUPITER_STILL, 7).getPath().endsWith("jupiter_07.png"));
    }
}
