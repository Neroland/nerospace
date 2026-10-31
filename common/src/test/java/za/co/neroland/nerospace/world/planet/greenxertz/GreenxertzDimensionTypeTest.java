package za.co.neroland.nerospace.world.planet.greenxertz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.ClockSource;

/**
 * Greenxertz keeps its real clock: {@code nerospace:greenxertz} is a faithful copy of the vanilla
 * {@code minecraft:overworld} dimension type of the Minecraft version under test (the vanilla JSON is read from the
 * game jar on the test classpath, so each cell checks against its own version), changing only {@code skybox} and the
 * planet's colour attributes (sky, fog, cloud, sunrise colour and star brightness: the green sky). On 1.21.6–1.21.10
 * the cloud height is absent to disable clouds. Clock, bed,
 * raid-darkness and spawn-relevant fields are also asserted one by one. Before 26.1 the build rewrites the type into
 * that line's format (stonecutter.gradle, legacyWorldgen), so the file under test here is the rewritten one.
 */
class GreenxertzDimensionTypeTest {

    /** The only keys the Greenxertz type may add to vanilla overworld's, with their required value. */
    private static final Map<String, String> ALLOWED_EXTRA = Map.of("skybox", "overworld");
    /** Visual attributes the planet palette may set or override; every other attribute stays vanilla. */
    private static final Set<String> PALETTE = Set.of("minecraft:visual/sky_color", "minecraft:visual/fog_color",
            "minecraft:visual/cloud_color", "minecraft:visual/sunrise_sunset_color", "minecraft:visual/star_brightness");

    private static JsonObject load(String path) throws IOException {
        try (InputStream in = GreenxertzDimensionTypeTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            JsonElement e = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return e.getAsJsonObject();
        }
    }

    @Test
    void isVanillaOverworldForThisVersionExceptSkybox() throws IOException {
        JsonObject vanilla = load("/data/minecraft/dimension_type/overworld.json");
        JsonObject ours = load("/data/nerospace/dimension_type/greenxertz.json");
        for (Map.Entry<String, JsonElement> e : vanilla.entrySet()) {
            //? if >=1.21.6 && <1.21.11 {
            /*if (e.getKey().equals("cloud_height")) {
                assertFalse(ours.has("cloud_height"), "planet clouds are disabled");
                continue;
            }
            *///?}
            if (e.getKey().equals("attributes")) {
                JsonObject va = e.getValue().getAsJsonObject();
                JsonObject oa = ours.getAsJsonObject("attributes");
                for (Map.Entry<String, JsonElement> a : va.entrySet()) {
                    if (!PALETTE.contains(a.getKey())) {
                        assertEquals(a.getValue(), oa.get(a.getKey()), "differs from vanilla overworld: " + a.getKey());
                    }
                }
                for (String k : oa.keySet()) {
                    // NeoForge 26.3 also selects the planet sky renderer (NeoForge-only overlay).
                    boolean sky = k.equals("neoforge:custom_skybox")
                            && "nerospace:planet".equals(oa.get(k).getAsString());
                    assertTrue(va.has(k) || PALETTE.contains(k) || sky, "unexpected extra attribute " + k);
                }
                continue;
            }
            assertEquals(e.getValue(), ours.get(e.getKey()), "differs from vanilla overworld: " + e.getKey());
        }
        for (String k : ours.keySet()) {
            if (!vanilla.has(k)) {
                assertTrue(ALLOWED_EXTRA.containsKey(k), "unexpected extra field " + k);
                assertEquals(ALLOWED_EXTRA.get(k), ours.get(k).getAsString(), k);
            }
        }
    }

    @Test
    void clockBedAndRaidFieldsAreOverworlds() throws IOException {
        JsonObject vanilla = load("/data/minecraft/dimension_type/overworld.json");
        JsonObject ours = load("/data/nerospace/dimension_type/greenxertz.json");
        assertFalse(ours.has("has_fixed_time") && ours.get("has_fixed_time").getAsBoolean(), "real clock");
        //? if >=26.1 {
        assertEquals("minecraft:overworld", ours.get("default_clock").getAsString());
        //?}
        assertEquals(vanilla.get("timelines"), ours.get("timelines"));
        for (String k : Set.of("ambient_light", "has_skylight", "has_ceiling", "monster_spawn_light_level",
                "monster_spawn_block_light_limit", "infiniburn")) {
            assertEquals(vanilla.get(k), ours.get(k), k);
        }
        //? if >=1.21.11 {
        JsonObject attrs = ours.getAsJsonObject("attributes");
        assertEquals(vanilla.getAsJsonObject("attributes").get("minecraft:gameplay/bed_rule"),
                attrs.get("minecraft:gameplay/bed_rule"), "bed rule: sleeping still skips the night");
        assertEquals("when_dark", attrs.getAsJsonObject("minecraft:gameplay/bed_rule").get("can_sleep").getAsString());
        //?} else {
        /*// Before 1.21.11 the build rewrites the type into the old field set: no fixed time, beds and raids as overworld's.
        assertFalse(ours.has("fixed_time"), "real clock");
        for (String k : Set.of("bed_works", "natural", "has_raids", "effects")) {
            assertEquals(vanilla.get(k), ours.get(k), k + ": sleeping still skips the night");
        }
        *///?}
        assertEquals(-64, ours.get("min_y").getAsInt());
        assertEquals(384, ours.get("height").getAsInt());
        assertEquals(384, ours.get("logical_height").getAsInt());
        assertEquals(1.0, ours.get("coordinate_scale").getAsDouble());
    }

    @Test
    void dimensionUsesTheNewTypeAndPlanetClockMirrorsLevelTime() throws IOException {
        JsonObject dim = load("/data/nerospace/dimension/greenxertz.json");
        assertEquals("nerospace:greenxertz", dim.get("type").getAsString());
        assertEquals(24_000L, GreenxertzPlanet.INSTANCE.traits().dayLengthTicks());
        assertEquals(ClockSource.LEVEL_TIME, GreenxertzPlanet.INSTANCE.clockSource());
    }
}
