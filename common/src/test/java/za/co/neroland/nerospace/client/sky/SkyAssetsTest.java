package za.co.neroland.nerospace.client.sky;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The planet skies, particles and ambience are data: this pins that every file they name exists, that every sky
 * profile decodes through {@link SkyProfile#CODEC} (and that profiles written before the 2.0.0 fields still do), that
 * the dimension-type colour floor uses the formats the 26.x attribute codecs accept, and that the NeoForge 26.3
 * dimension-type overlay is exactly the shared file plus the planet skybox attribute
 * ({@code tools/gen_sky_overlays.py}).
 */
class SkyAssetsTest {

    private static final List<String> SKIES = List.of("luna", "mars", "cindara", "glacira", "greenxertz", "nyxara",
            "station");
    private static final List<String> PARTICLES = List.of("dust", "ash", "spore", "diamond_dust", "ember",
            "nebula_mote");
    /** Textures the 26.3 sky renderer binds itself (not named in a profile). */
    private static final List<String> RENDERER_TEXTURES = List.of("horizon_ramp", "stars", "aurora_0", "aurora_1");
    /** Entity skins and glow layers the renderers name. */
    private static final List<String> ENTITY_TEXTURES = List.of("moon_hopper", "dust_strider", "dusk_moth",
            "dusk_moth_glow", "salvage_drone", "salvage_drone_glow", "dust_devil", "alien_villager_cindara",
            "alien_villager_cindara_glow", "alien_villager_glacira", "alien_villager_glacira_glow",
            "alien_villager_duskborn", "alien_villager_duskborn_glow", "alien_villager_glow");
    /**
     * Vanilla sound events the ambience aliases, each checked present in the 26.1.2, 26.2 and 26.3 SoundEvents
     * registries. A new alias must be checked the same way and added here.
     */
    private static final Set<String> VANILLA_EVENTS = Set.of("ambient.soul_sand_valley.loop",
            "ambient.basalt_deltas.additions", "item.elytra.flying", "ambient.basalt_deltas.loop", "block.lava.ambient",
            "block.fire.ambient", "ambient.basalt_deltas.mood", "ambient.nether_wastes.loop",
            "block.amethyst_block.chime", "block.powder_snow.step", "block.amethyst_block.resonate",
            "ambient.crimson_forest.loop", "ambient.crimson_forest.additions", "block.big_dripleaf.tilt_down",
            "ambient.crimson_forest.mood", "ambient.warped_forest.loop", "ambient.warped_forest.additions",
            "ambient.warped_forest.mood", "block.beacon.ambient", "block.respawn_anchor.charge", "block.piston.extend",
            "block.iron_door.open");
    private static final Set<String> ARGB_ATTRIBUTES = Set.of("minecraft:visual/cloud_color",
            "minecraft:visual/sunrise_sunset_color");
    private static final Set<String> RGB_ATTRIBUTES = Set.of("minecraft:visual/sky_color",
            "minecraft:visual/fog_color", "minecraft:visual/ambient_light_color");

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

    private static JsonObject read(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static SkyProfile decode(JsonElement json) {
        return SkyProfile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    @Test
    @DisplayName("every sky profile decodes, and its textures exist")
    void skyProfiles() throws IOException {
        Path a = assets();
        for (String sky : SKIES) {
            SkyProfile p = decode(read(a.resolve("sky/" + sky + ".json")));
            assertFalse(p.vanillaSkybox(), sky + " still asks for the vanilla sky");
            assertTrue(p.gradient().size() >= 2, sky + " has no gradient");
            List<SkyProfile.Body> bodies = new ArrayList<>(p.sun());
            bodies.addAll(p.moons());
            bodies.addAll(p.bodies());
            assertFalse(p.sun().isEmpty(), sky + " has no sun");
            for (SkyProfile.Body b : bodies) {
                assertTrue(b.texture().isPresent(), sky + "/" + b.id() + " has no texture");
                assertTexture(a, b.texture().get().getPath(), sky + "/" + b.id());
            }
            p.galaxy().flatMap(SkyProfile.Galaxy::texture).ifPresent(t -> assertTexture(a, t.getPath(), sky + " galaxy"));
            for (String key : p.weatherTints().keySet()) {
                assertNotNull(za.co.neroland.nerospace.world.planet.WeatherEvent.valueOf(key), key);
            }
            // The scene must compute at every phase without throwing, with sane alphas.
            for (int i = 0; i <= 20; i++) {
                SkyScene s = SkyScene.compute(p, i / 20.0, i / 20.0, 0.5, "NONE", 0.0F, 0.0, 2048, i * 600.0);
                assertTrue(s.starAlpha >= 0.0F && s.starAlpha <= 1.0F, sky);
                assertEquals(bodies.size(), s.bodies.size(), sky);
            }
        }
        for (String t : RENDERER_TEXTURES) {
            assertTexture(a, "textures/sky/" + t + ".png", "renderer");
        }
    }

    @Test
    @DisplayName("profiles written before the 2.0.0 fields still decode")
    void oldProfilesDecode() {
        SkyProfile placeholder = decode(JsonParser.parseString("{\"vanillaSkybox\": true}"));
        assertTrue(placeholder.vanillaSkybox());
        assertTrue(placeholder.gradient().isEmpty());
        assertTrue(placeholder.positionBlend().isEmpty());
        assertTrue(placeholder.terminatorScroll().isEmpty());
        SkyProfile old = decode(JsonParser.parseString("{\"background\": {\"zenith\": \"#102030\", \"blend\": "
                + "\"constant\"}, \"stars\": {\"count\": 10}, \"sun\": [{\"id\": \"s\", \"size\": 3.0}]}"));
        assertEquals(0x102030, SkyScene.compute(old, 0.25, 0.25, 1.0, "NONE", 0.0F, 0.0, 2048, 0.0).zenith);
        assertEquals(SkyProfile.VANILLA, decode(JsonParser.parseString("{\"vanillaSkybox\": true, \"clouds\": "
                + "\"none\"}")));
    }

    @Test
    @DisplayName("body angles follow the contract: east at 0, high at 0.25, west at 0.5; fast moons reversed")
    void bodyAngles() {
        SkyProfile.Body sun = body(1.0F, 0.0F, 0.0F);
        float[] rise = SkyScene.bodyAngles(sun, 0.0);
        assertEquals(270.0F, rise[0], 0.5F);
        assertEquals(0.0F, rise[1], 0.5F);
        assertEquals(90.0F, SkyScene.bodyAngles(sun, 0.25)[1], 0.5F);
        float[] set = SkyScene.bodyAngles(sun, 0.5);
        assertEquals(90.0F, set[0], 0.5F);
        assertEquals(0.0F, set[1], 0.5F);
        // Phobos (0.32 d) rises in the west.
        assertEquals(90.0F, SkyScene.bodyAngles(body(0.32F, 0.0F, 0.0F), 0.0)[0], 0.5F);
        // Inclination: horizon crossing at the inclination, culmination beside the zenith (south for positive).
        SkyProfile.Body inclined = body(1.0F, 0.0F, 6.0F);
        assertEquals(6.0F, SkyScene.bodyAngles(inclined, 0.0)[1], 0.5F);
        float[] noon = SkyScene.bodyAngles(inclined, 0.25);
        assertEquals(84.0F, noon[1], 0.5F);
        float[] noonDir = SkyScene.direction(noon[0], noon[1]);
        assertTrue(noonDir[2] > 0.05F && Math.abs(noonDir[0]) < 1e-3F, "an inclined noon leans south");
        // Luna's Earth (offset 0.22) rises at clock phase 0.78.
        assertEquals(12.0F, SkyScene.bodyAngles(body(1.0F, 0.22F, 12.0F), 0.78)[1], 0.5F);
        // Fixed angle wins.
        SkyProfile.Body fixed = new SkyProfile.Body("x", java.util.Optional.empty(), 4.0F, "#FFFFFF", 0.0F, 0.0F,
                0.0F, 0.0F, java.util.Optional.of(new SkyProfile.FixedAngle(90.0F, 2.0F)), false);
        assertEquals(90.0F, SkyScene.bodyAngles(fixed, 0.6)[0], 1e-4F);
        assertEquals(2.0F, SkyScene.bodyAngles(fixed, 0.6)[1], 1e-4F);
        // Yaw convention: 90 = west (-X), 270 = east (+X).
        assertTrue(SkyScene.direction(90.0F, 0.0F)[0] < -0.99F);
        assertTrue(SkyScene.direction(270.0F, 0.0F)[0] > 0.99F);
    }

    @Test
    @DisplayName("no body jumps when the clock phase wraps at sunrise, and every orbiting body rises")
    void bodiesContinuousAcrossTheDayWrap() throws IOException {
        Path a = assets();
        for (String sky : SKIES) {
            SkyProfile p = decode(read(a.resolve("sky/" + sky + ".json")));
            List<SkyProfile.Body> bodies = new ArrayList<>(p.sun());
            bodies.addAll(p.moons());
            bodies.addAll(p.bodies());
            for (SkyProfile.Body b : bodies) {
                String where = sky + "/" + b.id();
                // Across each of the first 30 sunrises (phase 1 -> 0) the body moves less than 1 degree,
                // through the same path the renderer uses (wrapped phase for colours, continuous days for bodies).
                for (int day = 1; day <= 30; day++) {
                    double before = day - 1e-4;
                    double after = day + 1e-4;
                    SkyScene s0 = SkyScene.compute(p, before - Math.floor(before), before, 0.5, "NONE", 0.0F, 0.0,
                            2048, 0.0);
                    SkyScene s1 = SkyScene.compute(p, after - Math.floor(after), after, 0.5, "NONE", 0.0F, 0.0, 2048,
                            0.0);
                    SkyScene.BodyDraw d0 = find(s0, b.id());
                    SkyScene.BodyDraw d1 = find(s1, b.id());
                    assertTrue(separation(d0, d1) < 1.0, where + " jumps " + separation(d0, d1) + " deg at sunrise "
                            + day);
                }
                if (b.orbitPeriodDays() == 0.0F || b.fixedAngle().isPresent()) {
                    continue;
                }
                // Every orbiting body is above the horizon at some point within two of its own periods.
                double span = Math.max(2.0, 2.0 * b.orbitPeriodDays());
                boolean rises = false;
                for (double d = 0.0; d <= span && !rises; d += 0.005) {
                    rises = SkyScene.bodyAngles(b, d)[1] > 1.0F;
                }
                assertTrue(rises, where + " never rises");
            }
        }
        // Cindara: the dark moon meets the red giant every day at 0.45 and is below the west horizon at dawn.
        SkyProfile cindara = decode(read(a.resolve("sky/cindara.json")));
        SkyProfile.Body sun = cindara.sun().get(0);
        SkyProfile.Body moon = cindara.moons().get(0);
        for (int day = 0; day < 4; day++) {
            float[] s = SkyScene.bodyAngles(sun, day + 0.45);
            float[] m = SkyScene.bodyAngles(moon, day + 0.45);
            assertTrue(separation(s, m) < 0.5, "Cindara conjunction on day " + day);
            float[] dawn = SkyScene.bodyAngles(moon, day + 0.0);
            assertTrue(dawn[1] < -20.0F, "Cindara dark moon above the horizon at dawn " + day);
        }
    }

    private static SkyScene.BodyDraw find(SkyScene s, String id) {
        return s.bodies.stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
    }

    private static double separation(SkyScene.BodyDraw a, SkyScene.BodyDraw b) {
        return separation(new float[] {a.yaw(), a.pitch()}, new float[] {b.yaw(), b.pitch()});
    }

    private static double separation(float[] a, float[] b) {
        float[] u = SkyScene.direction(a[0], a[1]);
        float[] v = SkyScene.direction(b[0], b[1]);
        double dot = Math.max(-1.0, Math.min(1.0, u[0] * v[0] + u[1] * v[1] + u[2] * v[2]));
        return Math.toDegrees(Math.acos(dot));
    }

    private static SkyProfile.Body body(float period, float offset, float incl) {
        return new SkyProfile.Body("b", java.util.Optional.empty(), 5.0F, "#FFFFFF", 0.0F, period, offset, incl,
                java.util.Optional.empty(), false);
    }

    @Test
    @DisplayName("gradient keys wrap and position blend picks the side")
    void gradientAndBlend() throws IOException {
        List<SkyProfile.GradientKey> keys = List.of(new SkyProfile.GradientKey(0.25F, "#FF0000", "#FF0000", "#FF0000"),
                new SkyProfile.GradientKey(0.75F, "#0000FF", "#0000FF", "#0000FF"));
        assertEquals(0xFF0000, SkyScene.gradientAt(keys, 0.25)[0]);
        assertEquals(0x0000FF, SkyScene.gradientAt(keys, 0.75)[0]);
        // Wraps: 0.0 is halfway between 0.75 and 1.25.
        assertEquals(SkyScene.lerpRgb(0x0000FF, 0xFF0000, 0.5F), SkyScene.gradientAt(keys, 0.0)[0]);
        SkyProfile nyx = decode(read(assets().resolve("sky/nyxara.json")));
        SkyProfile.PositionBlend b = nyx.positionBlend().orElseThrow();
        assertEquals(SkyProfile.rgb(b.west().horizon()), SkyScene.compute(nyx, 0.0, 0.0, 0.6, "NONE", 0.0F, -5000.0, 2048,
                0.0).horizon);
        assertEquals(SkyProfile.rgb(b.band().horizon()), SkyScene.compute(nyx, 0.0, 0.0, 0.6, "NONE", 0.0F, 0.0, 2048,
                0.0).horizon);
        assertEquals(SkyProfile.rgb(b.east().horizon()), SkyScene.compute(nyx, 0.0, 0.0, 0.6, "NONE", 0.0F, 5000.0, 2048,
                0.0).horizon);
    }

    @Test
    @DisplayName("every ambient particle type has its definition and four frames")
    void particles() throws IOException {
        Path a = assets();
        for (String type : PARTICLES) {
            JsonObject def = read(a.resolve("particles/" + type + ".json"));
            assertEquals(4, def.getAsJsonArray("textures").size(), type);
            for (JsonElement t : def.getAsJsonArray("textures")) {
                String id = t.getAsString();
                assertTexture(a, "textures/particle/" + id.substring(id.indexOf(':') + 1) + ".png", type);
            }
        }
    }

    @Test
    @DisplayName("every entity skin and glow layer the renderers name exists")
    void entityTextures() {
        Path a = assets();
        for (String t : ENTITY_TEXTURES) {
            assertTexture(a, "textures/entity/" + t + ".png", t);
        }
    }

    @Test
    @DisplayName("ambient sounds: every event has a subtitle, aliases a checked vanilla event, and every reference exists")
    void sounds() throws IOException {
        Path a = assets();
        JsonObject sounds = read(a.resolve("sounds.json"));
        JsonObject en = read(a.resolve("lang/en_us.json"));
        int ambient = 0;
        for (Map.Entry<String, JsonElement> e : sounds.entrySet()) {
            JsonObject ev = e.getValue().getAsJsonObject();
            if (ev.has("subtitle")) {
                assertTrue(en.has(ev.get("subtitle").getAsString()), "no subtitle text for " + e.getKey());
            }
            if (!e.getKey().startsWith("ambient.")) {
                continue;
            }
            ambient++;
            for (JsonElement s : ev.getAsJsonArray("sounds")) {
                JsonObject o = s.getAsJsonObject();
                assertEquals("event", o.get("type").getAsString(), e.getKey());
                String name = o.get("name").getAsString().replace("minecraft:", "");
                assertTrue(VANILLA_EVENTS.contains(name), e.getKey() + " aliases an unchecked event " + name);
            }
        }
        assertEquals(22, ambient);
        // Every nerospace:* sound named by a dimension type or biome attribute exists in sounds.json.
        Path data = repo().resolve("common/src/main/resources/data/nerospace");
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.walk(data.resolve("dimension_type"))) {
            s.filter(p -> p.toString().endsWith(".json")).forEach(files::add);
        }
        try (Stream<Path> s = Files.walk(data.resolve("worldgen/biome"))) {
            s.filter(p -> p.toString().endsWith(".json")).forEach(files::add);
        }
        for (Path f : files) {
            JsonObject attrs = read(f).getAsJsonObject("attributes");
            if (attrs != null && attrs.has("minecraft:visual/ambient_particles")) {
                // 26.x has no "append" modifier for this list: it must be a plain list of registered types.
                JsonElement list = attrs.get("minecraft:visual/ambient_particles");
                assertTrue(list.isJsonArray(), f.getFileName() + " ambient_particles must be a list");
                for (JsonElement e : list.getAsJsonArray()) {
                    String type = e.getAsJsonObject().getAsJsonObject("particle").get("type").getAsString();
                    assertTrue(!type.startsWith("nerospace:") || PARTICLES.contains(type.substring(10)),
                            f.getFileName() + " names " + type);
                }
            }
            for (String ref : soundRefs(read(f))) {
                assertTrue(sounds.has(ref.substring("nerospace:".length())), f.getFileName() + " names " + ref);
            }
        }
    }

    private static List<String> soundRefs(JsonElement e) {
        List<String> out = new ArrayList<>();
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() && e.getAsString().startsWith("nerospace:ambient.")) {
            out.add(e.getAsString());
        } else if (e.isJsonObject()) {
            e.getAsJsonObject().entrySet().forEach(x -> out.addAll(soundRefs(x.getValue())));
        } else if (e.isJsonArray()) {
            e.getAsJsonArray().forEach(x -> out.addAll(soundRefs(x)));
        }
        return out;
    }

    @Test
    @DisplayName("dimension-type colour floor uses the 26.x attribute formats, and the NeoForge 26.3 overlay is in sync")
    void dimensionTypes() throws IOException {
        Path r = repo();
        Assumptions.assumeTrue(r != null, "repo not found");
        Path base = r.resolve("common/src/main/resources/data/nerospace/dimension_type");
        Path shared263 = r.resolve("common/src/main/resources-26.3/data/nerospace/dimension_type");
        Path neo263 = r.resolve("neoforge/src/main/resources-26.3/data/nerospace/dimension_type");
        Gson gson = new GsonBuilder().create();
        List<Path> types = new ArrayList<>();
        try (Stream<Path> s = Files.list(base)) {
            s.filter(p -> p.toString().endsWith(".json")).sorted().forEach(types::add);
        }
        assertEquals(7, types.size());
        for (Path t : types) {
            for (Path f : new Path[] {t, shared263.resolve(t.getFileName())}) {
                if (!Files.isRegularFile(f)) {
                    continue;
                }
                JsonObject attrs = read(f).getAsJsonObject("attributes");
                assertNotNull(attrs, f + " has no attributes");
                for (Map.Entry<String, JsonElement> e : attrs.entrySet()) {
                    String k = e.getKey();
                    if (ARGB_ATTRIBUTES.contains(k)) {
                        assertTrue(e.getValue().getAsString().matches("#[0-9A-Fa-f]{8}"), f + " " + k);
                    } else if (RGB_ATTRIBUTES.contains(k)) {
                        assertTrue(e.getValue().getAsString().matches("#[0-9A-Fa-f]{6}"), f + " " + k);
                    }
                    // Fog distances are blocks; a fraction here would put a fog wall at the player's face.
                    assertFalse(k.endsWith("fog_start_distance") || k.endsWith("fog_end_distance"), f + " " + k);
                    if (k.equals("minecraft:visual/moon_phase")) {
                        assertTrue(e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString(), f + " " + k);
                    }
                }
            }
            Path src = Files.isRegularFile(shared263.resolve(t.getFileName())) ? shared263.resolve(t.getFileName()) : t;
            JsonObject expected = read(src);
            JsonObject attrs = expected.getAsJsonObject("attributes").deepCopy();
            attrs.addProperty("neoforge:custom_skybox", "nerospace:planet");
            expected.add("attributes", attrs);
            Path overlay = neo263.resolve(t.getFileName());
            assertTrue(Files.isRegularFile(overlay), "missing NeoForge 26.3 overlay " + overlay
                    + " (run tools/gen_sky_overlays.py)");
            assertEquals(canonical(gson, expected), canonical(gson, read(overlay)),
                    "stale NeoForge 26.3 overlay " + overlay + " (run tools/gen_sky_overlays.py)");
        }
        // The NeoForge-only attribute must never reach the shared resources.
        for (Path t : types) {
            assertFalse(read(t).getAsJsonObject("attributes").has("neoforge:custom_skybox"), t.toString());
        }
    }

    private static String canonical(Gson gson, JsonElement e) {
        return gson.toJson(sort(e));
    }

    private static JsonElement sort(JsonElement e) {
        if (e.isJsonObject()) {
            Map<String, JsonElement> m = new TreeMap<>();
            e.getAsJsonObject().entrySet().forEach(x -> m.put(x.getKey(), sort(x.getValue())));
            JsonObject o = new JsonObject();
            m.forEach(o::add);
            return o;
        }
        return e;
    }

    private static void assertTexture(Path assets, String path, String what) {
        Path png = assets.resolve(path);
        assertTrue(Files.isRegularFile(png), what + ": missing " + png);
    }
}
