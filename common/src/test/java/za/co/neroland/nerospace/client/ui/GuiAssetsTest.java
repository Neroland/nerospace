package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.progression.StarGuide;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * The GUI textures the screens and HUD blit exist at the size they are drawn from, and every lang key the Star
 * Guide, Station Console, Launch Controller and HUD look up exists in every shipped language.
 */
class GuiAssetsTest {

    private static BufferedImage png(String name) throws IOException {
        try (InputStream in = GuiAssetsTest.class.getResourceAsStream("/assets/nerospace/textures/gui/" + name)) {
            assertNotNull(in, "missing texture " + name);
            return ImageIO.read(in);
        }
    }

    @Test
    @DisplayName("GUI sheets exist at their blit sizes and the panels' regions are painted")
    void textures() throws IOException {
        Object[][] sheets = {
            { "star_guide.png", 256, 256, StarGuideLayout.PANEL_W, StarGuideLayout.PANEL_H },
            { "station_console.png", 256, 256, ConsoleLayout.PANEL_W, ConsoleLayout.PANEL_H },
            { "passive_generator.png", 256, 256, 176, 166 },
            { "nerosium_grinder.png", 256, 256, 176, 166 },
            { "rocket.png", 256, 256, 0, 0 },
            { "hud_glyphs.png", Glyphs.SHEET_W, Glyphs.SHEET_H, 0, 0 },
            { "widgets.png", 256, 64, 0, 0 },
            { "oxygen_hud_icon.png", 16, 16, 0, 0 },
        };
        for (Object[] s : sheets) {
            BufferedImage img = png((String) s[0]);
            assertEquals((int) s[1], img.getWidth(), s[0] + " width");
            assertEquals((int) s[2], img.getHeight(), s[0] + " height");
            int w = (int) s[3];
            int h = (int) s[4];
            if (w > 0) {
                // Panel corners inside the blitted region are opaque (the hull frame).
                assertTrue((img.getRGB(1, 1) >>> 24) == 255, s[0] + " top-left");
                assertTrue((img.getRGB(w - 2, h - 2) >>> 24) == 255, s[0] + " bottom-right");
            }
        }
        // The panel wells the screens draw into are in the PNGs (darker than the hull frame).
        BufferedImage guide = png("star_guide.png");
        for (int[] well : new int[][] { StarGuideLayout.RAIL_WELL, StarGuideLayout.STEP_WELL, StarGuideLayout.TEXT_WELL }) {
            int c = guide.getRGB(well[0] + well[2] / 2, well[1] + well[3] / 2);
            assertTrue(luma(c) < 40, "star guide well at " + well[0] + "," + well[1]);
        }
        BufferedImage console = png("station_console.png");
        int c = console.getRGB(ConsoleLayout.PAGE_WELL[0] + 50, ConsoleLayout.PAGE_WELL[1] + 50);
        assertTrue(luma(c) < 40, "console page well");
    }

    private static int luma(int argb) {
        return (((argb >> 16) & 0xFF) * 3 + ((argb >> 8) & 0xFF) * 6 + (argb & 0xFF)) / 10;
    }

    /** Every key the reworked screens and the HUD use. */
    static List<String> usedKeys() {
        List<String> keys = new ArrayList<>(List.of(
                "gui.nerospace.star_guide.progress", "gui.nerospace.star_guide.progress.tooltip",
                "gui.nerospace.star_guide.bonus", "gui.nerospace.star_guide.bonus.tooltip",
                "gui.nerospace.star_guide.scroll_chapters", "gui.nerospace.star_guide.complete",
                "gui.nerospace.launch_controller.prev_dest", "gui.nerospace.launch_controller.next_dest",
                "gui.nerospace.launch_controller.needed.pads", "gui.nerospace.launch_controller.needed.wall",
                "gui.nerospace.launch_controller.needed.gantry", "gui.nerospace.launch_controller.pad",
                "gui.nerospace.launch_controller.wall", "gui.nerospace.launch_controller.gantry",
                "gui.nerospace.launch_controller.hub.fuel", "gui.nerospace.launch_controller.hub.oxygen",
                "gui.nerospace.launch_controller.hub.power", "gui.nerospace.launch_controller.built_tier",
                "gui.nerospace.launch_controller.preview", "gui.nerospace.launch_controller.build",
                "gui.nerospace.launch_controller.mode_build", "gui.nerospace.launch_controller.mode_launch",
                "gui.nerospace.station_console.tab.overview", "gui.nerospace.station_console.tab.overview.short",
                "gui.nerospace.station_console.tab.members", "gui.nerospace.station_console.cost",
                "gui.nerospace.station_console.cost.short", "gui.nerospace.station_console.pad_missing",
                "gui.nerospace.station_console.unclaimed", "gui.nerospace.station_console.visitor",
                "gui.nerospace.station_console.repair", "gui.nerospace.station_console.on",
                "gui.nerospace.station_console.off",
                "hud.nerospace.oxygen", "hud.nerospace.suit_rad", "hud.nerospace.hazard_radiation",
                "hud.nerospace.geiger", "hud.nerospace.clock.tooltip"));
        for (StarGuide.Chapter chapter : StarGuide.CHAPTERS) {
            keys.add(chapter.titleKey());
            if (chapter.id().startsWith(StarGuide.WORLDS_PREFIX)) {
                keys.add("gui.nerospace.star_guide.rail." + chapter.id());
            }
        }
        for (WeatherEvent e : WeatherEvent.values()) {
            keys.add("hud.nerospace.weather." + e.name().toLowerCase(Locale.ROOT));
        }
        for (int i = 0; i < 100; i++) {
            keys.add(ClockDial.phaseKey(i / 100.0, List.of(new ClockDial.Window(0.4, 0.5, ClockDial.BAD, true))));
        }
        return keys;
    }

    @Test
    @DisplayName("every lang key the screens and HUD use exists in en_us, it_it and zh_cn")
    void langKeys() throws IOException {
        List<String> keys = usedKeys();
        for (String locale : new String[] { "en_us", "it_it", "zh_cn" }) {
            try (InputStream in = GuiAssetsTest.class.getResourceAsStream("/assets/nerospace/lang/" + locale + ".json")) {
                assertNotNull(in, locale);
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String k : keys) {
                    assertTrue(o.has(k) && !o.get(k).getAsString().isBlank(), locale + " missing " + k);
                }
            }
        }
    }
}
