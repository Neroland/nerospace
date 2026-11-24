package za.co.neroland.nerospace.client.sky;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The sky hooks of 2.4.0: the body-style texture choice, the {@code shaderDimension} profile field, the body lists
 * of the station and Mars, the square texture set, and source-level checks on the shared draw path and its hooks
 * (they need a GPU, so the contract is pinned on the source: a level without a planet profile is never handled,
 * mesh caches are released on a profile reload, and the mixin only cancels the vanilla sky after the planet sky drew).
 */
class SkyHooksTest {

    private static final List<String> SKIES = List.of("luna", "mars", "cindara", "glacira", "greenxertz", "nyxara",
            "station");

    private static Path repo() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("common/src/main/resources/assets/nerospace"))) {
                return p;
            }
        }
        throw new IllegalStateException("repo not found");
    }

    private static String text(String rel) throws IOException {
        return Files.readString(repo().resolve(rel), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static JsonObject json(String rel) throws IOException {
        return JsonParser.parseString(text(rel)).getAsJsonObject();
    }

    private static SkyProfile decode(JsonElement json) {
        return SkyProfile.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
    }

    private static SkyProfile profile(String sky) throws IOException {
        return decode(json("common/src/main/resources/assets/nerospace/sky/" + sky + ".json"));
    }

    /** The text of the method that starts at {@code signature}, up to the next method-level comment or member. */
    private static String method(String src, String signature) {
        int start = src.indexOf(signature);
        assertTrue(start >= 0, "missing " + signature);
        int end = src.indexOf("\n    }\n", start);
        assertTrue(end > start, "unterminated " + signature);
        return src.substring(start, end);
    }

    // ------------------------------------------------------------------------------------------- body style

    @Test
    @DisplayName("sky.bodyStyle parses to globe unless it says square")
    void bodyStyleByName() {
        assertEquals(SkyBodyStyle.SQUARE, SkyBodyStyle.byName("square"));
        assertEquals(SkyBodyStyle.SQUARE, SkyBodyStyle.byName("  Square "));
        assertEquals(SkyBodyStyle.GLOBE, SkyBodyStyle.byName("globe"));
        assertEquals(SkyBodyStyle.GLOBE, SkyBodyStyle.byName(""));
        assertEquals(SkyBodyStyle.GLOBE, SkyBodyStyle.byName(null));
        assertEquals(SkyBodyStyle.GLOBE, SkyBodyStyle.byName("cube"));
    }

    @Test
    @DisplayName("the square style swaps a texture for its square variant, and keeps the globe when there is none")
    void bodyStyleSelection() {
        String globe = "textures/sky/moon_small.png";
        String square = "textures/sky/square/moon_small.png";
        assertEquals(square, SkyBodyStyle.squarePath(globe));
        assertEquals(square, SkyBodyStyle.select(globe, SkyBodyStyle.SQUARE, p -> true));
        // No square variant on disk: the globe texture stays.
        assertEquals(globe, SkyBodyStyle.select(globe, SkyBodyStyle.SQUARE, p -> false));
        // The globe style never asks whether a variant exists.
        assertEquals(globe, SkyBodyStyle.select(globe, SkyBodyStyle.GLOBE, p -> {
            throw new AssertionError("globe style looked for " + p);
        }));
        // Only textures directly in textures/sky/ can have a variant.
        for (String other : List.of(square, "textures/sky/sub/moon.png", "textures/entity/moon_hopper.png",
                "textures/sky/")) {
            assertEquals(other, SkyBodyStyle.squarePath(other));
            assertEquals(other, SkyBodyStyle.select(other, SkyBodyStyle.SQUARE, p -> true));
        }
    }

    @Test
    @DisplayName("every square texture has a globe counterpart, and the bodies the profiles draw have a square")
    void squareTextures() throws IOException {
        Path sky = repo().resolve("common/src/main/resources/assets/nerospace/textures/sky");
        Path square = sky.resolve("square");
        assertTrue(Files.isDirectory(square), "textures/sky/square is missing (tools/gen_sky_textures.py)");
        List<String> names;
        try (Stream<Path> files = Files.list(square)) {
            names = files.map(p -> p.getFileName().toString()).sorted().toList();
        }
        assertFalse(names.isEmpty());
        for (String name : names) {
            assertTrue(name.endsWith(".png"), name + " is not a texture");
            assertTrue(Files.isRegularFile(sky.resolve(name)), "square/" + name + " has no globe texture");
        }
        // Every sun, moon and planet a profile names, its limb glow and all sixteen night-shade frames.
        for (String s : SKIES) {
            SkyProfile p = profile(s);
            for (List<SkyProfile.Body> list : List.of(p.sun(), p.moons(), p.bodies())) {
                for (SkyProfile.Body b : list) {
                    String path = b.texture().orElseThrow().getPath();
                    String variant = SkyBodyStyle.squarePath(path);
                    assertTrue(Files.isRegularFile(sky.resolve(variant.substring(SkyBodyStyle.SKY_FOLDER.length()))),
                            s + "/" + b.id() + " has no square variant of " + path);
                }
            }
        }
        assertTrue(names.contains("limb_glow.png"));
        for (int i = 0; i < 16; i++) {
            assertTrue(names.contains(String.format("terminator_%02d.png", i)), "square night-shade frame " + i);
        }
    }

    // --------------------------------------------------------------------------------------------- profiles

    @Test
    @DisplayName("shaderDimension is optional: absent decodes to the default, present is kept, shipped profiles decode")
    void shaderDimension() throws IOException {
        SkyProfile absent = decode(JsonParser.parseString("{\"stars\": {\"count\": 10}}"));
        assertEquals(SkyProfile.DEFAULT_SHADER_DIMENSION, absent.shaderDimension());
        assertEquals("overworld", SkyProfile.VANILLA.shaderDimension());
        SkyProfile present = decode(JsonParser.parseString("{\"shaderDimension\": \"end\", \"stars\": {\"count\": 10}}"));
        assertEquals("end", present.shaderDimension());
        // The field is the only difference, and it survives a round trip.
        assertEquals(absent.stars(), present.stars());
        assertEquals(present, decode(SkyProfile.CODEC.encodeStart(JsonOps.INSTANCE, present).result().orElseThrow()));
        // Every world but Greenxertz asks the pack for its End look (Cindara and Glacira since 2026-11-21).
        Set<String> endLook = Set.of("station", "luna", "mars", "nyxara", "cindara", "glacira");
        for (String s : SKIES) {
            SkyProfile p = profile(s);
            assertEquals(endLook.contains(s) ? "end" : SkyProfile.DEFAULT_SHADER_DIMENSION, p.shaderDimension(), s);
        }
    }

    @Test
    @DisplayName("the station shows Earth, the Moon and the sun; Mars shows Phobos, Deimos, the sun and Earth as a star")
    void bodyLists() throws IOException {
        SkyProfile station = profile("station");
        assertEquals(1, station.sun().size());
        assertEquals(List.of("luna"), station.moons().stream().map(SkyProfile.Body::id).toList());
        assertEquals(List.of("homeworld"), station.bodies().stream().map(SkyProfile.Body::id).toList());
        // Earth is the large disc below; the Moon is small beside it.
        assertTrue(station.bodies().get(0).size() > 20.0F * station.moons().get(0).size());
        assertTrue(station.bodies().get(0).fixedAngle().orElseThrow().pitch() < 0.0F);

        SkyProfile luna = profile("luna");
        assertEquals(List.of("earth"), luna.bodies().stream().map(SkyProfile.Body::id).toList());

        SkyProfile mars = profile("mars");
        assertEquals(List.of("phobos", "deimos"), mars.moons().stream().map(SkyProfile.Body::id).toList());
        assertEquals(1, mars.sun().size());
        assertTrue(mars.sun().get(0).size() < station.sun().get(0).size(), "the Martian sun is the smaller one");
        SkyProfile.Decal earth = mars.decals().stream().filter(d -> "earth_star".equals(d.id())).findFirst()
                .orElseThrow();
        // An evening / morning star: it rides a fixed angle above the sun and shows only at night.
        assertEquals("sun", earth.anchor());
        assertTrue(earth.offsetPitch() > 5.0F && earth.offsetPitch() < 47.0F);
        assertTrue(earth.visibility().orElseThrow().night());
        assertTrue(earth.size() < 2.0F);
    }

    // ------------------------------------------------------------------------------------- source contracts

    @Test
    @DisplayName("a level without a planet profile is never handled, and never reaches a draw call")
    void levelWithoutProfileIsNotHandled() throws IOException {
        String src = text("common/src/main/java/za/co/neroland/nerospace/client/sky/PlanetSkyDraw.java");
        String render = method(src, "public boolean render(float partialTick");
        int noFrame = render.indexOf("if (frame == null) {\n            return false;");
        assertTrue(noFrame > 0, "render(...) must return false for a level without a planet frame");
        for (String later : List.of("build();", "draw(mc,", "RenderSystem.setProjectionMatrix", "PlanetSkyRenderer.scene(")) {
            int at = render.indexOf(later);
            assertTrue(at > noFrame, later + " must come after the no-profile return");
        }
        // The other two reasons to leave the sky to vanilla are checked before anything else.
        int gate = render.indexOf("if (this.failed || !ShaderCompat.drawPlanetSky()) {\n            return false;");
        assertTrue(gate > 0 && gate < noFrame);
        // true is returned in exactly two places: the fog-hidden sky on a planet (after the no-profile return, so a
        // vanilla level never has its sky swallowed) and after the draw.
        assertEquals(2, render.split("return true;", -1).length - 1);
        int fog = render.indexOf("if (SkyFluidRules.hidesPlanetSky(");
        assertTrue(fog > noFrame, "the fog check must come after the no-profile return");
        assertTrue(render.indexOf("return true;") > fog && render.indexOf("return true;") < render.indexOf("build();"),
                "the fog case returns before any mesh is built");
        assertTrue(render.lastIndexOf("return true;") > render.indexOf("draw(mc,"));
    }

    @Test
    @DisplayName("mesh caches are released on a sky-profile reload and when the sky disables itself")
    void meshCachesReleasedOnReload() throws IOException {
        String src = text("common/src/main/java/za/co/neroland/nerospace/client/sky/PlanetSkyDraw.java");
        String render = method(src, "public boolean render(float partialTick");
        int changed = render.indexOf("if (generation != this.profileGeneration) {\n            release();");
        assertTrue(changed > 0, "render(...) must release the meshes when SkyProfiles.generation() moved");
        assertTrue(render.indexOf("SkyProfiles.generation()") < changed);
        assertTrue(changed < render.indexOf("build();"), "the release must come before the meshes are rebuilt");
        assertTrue(render.indexOf("this.failed = true;") < render.lastIndexOf("release();"),
                "a render error must release the meshes too");
        String release = method(src, "private void release()");
        for (String cache : List.of("this.styled.clear();", "this.ramps.clear();", "this.stars.clear();",
                "this.starQuads.clear();", "this.clouds.clear();", "this.disc = null;", "this.quadTex = null;",
                "this.quadTexColor = null;", "this.galaxy = null;", "this.skyProjection = null;",
                "this.auroraBuffers[i] = null;", "this.auroraValid = false;")) {
            assertTrue(release.contains(cache), "release() must drop " + cache);
        }
    }

    @Test
    @DisplayName("the sky mixin only cancels the vanilla sky after the planet sky drew, and never beside a loader hook")
    void mixinFallsThroughToVanilla() throws IOException {
        String src = text("common/src/main/java/za/co/neroland/nerospace/mixin/sky/SkyRendererPlanetMixin.java");
        int injects = src.split("@Inject\\(", -1).length - 1;
        assertEquals(7, injects, "one 26.3 hook and six 26.1 / 26.2 hooks");
        assertEquals(injects, src.split("require = 0\\)", -1).length - 1, "every injection must be optional");
        assertEquals(injects, src.split("cancellable = true", -1).length - 1);
        assertEquals(injects, src.split("at = @At\\(\"HEAD\"\\)", -1).length - 1);
        // Every cancel sits directly under a condition that the planet sky drew.
        String[] parts = src.split("ci\\.cancel\\(\\);", -1);
        assertEquals(4, parts.length, "three cancels: the 26.3 hook, the first call and the later calls");
        for (int i = 0; i < parts.length - 1; i++) {
            String before = parts[i].stripTrailing();
            String condition = before.substring(before.lastIndexOf('\n') + 1).strip();
            assertTrue(condition.equals("if (PlanetSkyDraw.mixinDraws() && PlanetSkyDraw.renderCurrent(skyFog)) {")
                    || condition.equals("if (nerospace$handled) {"), "unconditional cancel after: " + condition);
        }
        assertTrue(src.contains("nerospace$handled = PlanetSkyDraw.mixinDraws() && PlanetSkyDraw.renderCurrent(null);"));
        // NeoForge 26.3 draws through its own hook and claims it, so the mixin stays out there.
        String neo = text("neoforge/src/main/java/za/co/neroland/nerospace/neoforge/NeoForgeSky.java");
        assertTrue(neo.indexOf("PlanetSkyDraw.claimLoaderHook();") > neo.indexOf("registerSkyboxRenderer(SKY_ID"));
        String draw = text("common/src/main/java/za/co/neroland/nerospace/client/sky/PlanetSkyDraw.java");
        assertTrue(method(draw, "public static boolean mixinDraws()").contains("return !loaderHook;"));
    }

    @Test
    @DisplayName("the sky mixin is listed from 26.1.2 only, client-side and optional, and every loader declares it")
    void mixinConfigs() throws IOException {
        JsonObject base = json("common/src/main/resources/nerospace-sky.mixins.json");
        JsonObject from261 = json("common/src/main/resources-26.1.2/nerospace-sky.mixins.json");
        for (JsonObject config : List.of(base, from261)) {
            assertFalse(config.get("required").getAsBoolean());
            assertEquals("za.co.neroland.nerospace.mixin.sky", config.get("package").getAsString());
            assertEquals(0, config.getAsJsonArray("mixins").size(), "the sky mixin is client-only");
            assertEquals(0, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
        }
        assertEquals(0, base.getAsJsonArray("client").size(), "nothing is hooked below 26.1");
        JsonArray client = from261.getAsJsonArray("client");
        assertEquals(1, client.size());
        assertEquals("SkyRendererPlanetMixin", client.get(0).getAsString());
        assertFalse(json("common/src/main/resources/nerospace-shaders.mixins.json").get("required").getAsBoolean());
        for (String manifest : List.of("fabric/src/main/templates/fabric.mod.json",
                "forge/src/main/templates/META-INF/mods.toml",
                "neoforge/src/main/templates/META-INF/neoforge.mods.toml")) {
            String m = text(manifest);
            for (String config : List.of("nerospace-common.mixins.json", "nerospace-sky.mixins.json",
                    "nerospace-shaders.mixins.json")) {
                assertEquals(1, m.split("\"" + config + "\"", -1).length - 1, manifest + " declares " + config);
            }
        }
    }
}
