package za.co.neroland.nerospace.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.planet.nyxara.worldgen.FixedXBands;

/** The creative destination compasses: the full family ships its assets, and each sets its holder down safely. */
class DestinationCompassTest {

    private static final String[] COMPASSES = { "station_compass", "greenxertz_compass", "cindara_compass",
        "glacira_compass", "luna_compass", "mars_compass", "nyxara_compass" };
    private static final String[] LANGS = { "en_us", "it_it", "zh_cn" };

    private static JsonObject json(String path) throws IOException {
        try (InputStream in = DestinationCompassTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    @DisplayName("every compass has an item definition, model, 16x16 texture, lang in every language and a highlight tag")
    void assets() throws IOException {
        JsonArray tools = json("/data/neroland/tags/item/highlight/tools.json").getAsJsonArray("values");
        for (String id : COMPASSES) {
            assertEquals("nerospace:item/" + id,
                    json("/assets/nerospace/items/" + id + ".json").getAsJsonObject("model").get("model").getAsString());
            assertEquals("nerospace:item/" + id, json("/assets/nerospace/models/item/" + id + ".json")
                    .getAsJsonObject("textures").get("layer0").getAsString());
            try (InputStream in = getClass().getResourceAsStream("/assets/nerospace/textures/item/" + id + ".png")) {
                assertNotNull(in, "missing texture " + id);
                BufferedImage img = ImageIO.read(in);
                assertEquals(16, img.getWidth());
                assertEquals(16, img.getHeight());
            }
            for (String lang : LANGS) {
                JsonObject l = json("/assets/nerospace/lang/" + lang + ".json");
                assertTrue(l.has("item.nerospace." + id), lang + " lacks " + id);
            }
            boolean tagged = false;
            for (JsonElement e : tools) {
                tagged |= e.isJsonPrimitive() && e.getAsString().equals("nerospace:" + id);
            }
            assertTrue(tagged, id + " is in the tools highlight tag");
        }
    }

    @Test
    @DisplayName("the compasses stay creative-only: no recipe and no recipe advancement")
    void creativeOnly() {
        for (String id : COMPASSES) {
            assertNull(getClass().getResource("/data/nerospace/recipe/" + id + ".json"), id + " recipe");
            assertNull(getClass().getResource("/data/nerospace/advancement/recipes/tools/" + id + ".json"),
                    id + " recipe advancement");
            assertNull(getClass().getResource("/data/nerospace/advancement/recipes/misc/" + id + ".json"),
                    id + " recipe advancement");
        }
    }

    @Test
    @DisplayName("Nyxara: the holder is set down inside the twilight band, clear of the ragged terminator")
    void nyxaraBand() {
        int width = FixedXBands.DEFAULT_WIDTH;
        int noise = FixedXBands.DEFAULT_EDGE_NOISE;
        assertEquals(100, CompassLanding.intoBand(100, width, 0, noise), "already in the band: unchanged");
        for (int x : new int[] { -30_000_000, -5000, -1100, 1100, 5000, 30_000_000 }) {
            int landed = CompassLanding.intoBand(x, width, 0, noise);
            for (int z = -2000; z <= 2000; z += 37) {
                assertEquals(FixedXBands.Side.BAND, FixedXBands.sideAt(landed, z, width, 0, noise, 12345L),
                        "x " + x + " landed at " + landed + ", z " + z);
            }
        }
        for (int w : new int[] { FixedXBands.MIN_WIDTH, 4096, FixedXBands.MAX_WIDTH }) {
            int landed = CompassLanding.intoBand(1_000_000, w, 300, noise);
            for (int z = -500; z <= 500; z += 41) {
                assertEquals(FixedXBands.Side.BAND, FixedXBands.sideAt(landed, z, w, 300, noise, 7L));
            }
        }
    }

    @Test
    @DisplayName("surface worlds (Luna, Mars) land on the ground; a groundless column gets a platform")
    void surface() {
        assertEquals(71, CompassLanding.feetY(70, -64));
        assertEquals(-1, CompassLanding.feetY(-64, -64));
        assertEquals(-1, CompassLanding.feetY(0, 0));
        assertFalse(CompassLanding.PLATFORM_Y < -64);
    }
}
