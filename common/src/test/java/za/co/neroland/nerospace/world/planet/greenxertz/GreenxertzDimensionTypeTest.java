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
 * game jar on the test classpath, so each cell checks against its own version), changing only {@code skybox}.
 * Clock, bed, raid-darkness and spawn-relevant fields are also asserted one by one.
 */
class GreenxertzDimensionTypeTest {

    /** The only keys the Greenxertz type may add to vanilla overworld's, with their required value. */
    private static final Map<String, String> ALLOWED_EXTRA = Map.of("skybox", "overworld");

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
        assertEquals("minecraft:overworld", ours.get("default_clock").getAsString());
        assertEquals(vanilla.get("timelines"), ours.get("timelines"));
        for (String k : Set.of("ambient_light", "has_skylight", "has_ceiling", "monster_spawn_light_level",
                "monster_spawn_block_light_limit", "infiniburn")) {
            assertEquals(vanilla.get(k), ours.get(k), k);
        }
        JsonObject attrs = ours.getAsJsonObject("attributes");
        assertEquals(vanilla.getAsJsonObject("attributes").get("minecraft:gameplay/bed_rule"),
                attrs.get("minecraft:gameplay/bed_rule"), "bed rule: sleeping still skips the night");
        assertEquals("when_dark", attrs.getAsJsonObject("minecraft:gameplay/bed_rule").get("can_sleep").getAsString());
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
