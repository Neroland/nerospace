package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The HUD clock dial and its phase label never share pixels with the status lines, even with all five lines up
 * (O2, suit, rad lining, weather, hazard), at either dial scale, on small and large GUIs, in every shipped language.
 */
class HudLayoutTest {

    /** GUI sizes (scaled width x height): GUI scale 4 on 854x480 up to scale 2 on 1920x1080. */
    private static final int[][] GUIS = { { 213, 120 }, { 320, 180 }, { 427, 240 }, { 480, 270 }, { 640, 360 },
        { 960, 540 } };

    /** Upper-bound advance of the vanilla font: 6 px per Latin glyph, 9 px per CJK glyph. */
    private static int width(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            w += cp >= 0x2E80 ? 9 : 6;
            i += Character.charCount(cp);
        }
        return w;
    }

    private static List<String> clockLabels() throws IOException {
        List<String> out = new ArrayList<>();
        for (String locale : new String[] { "en_us", "it_it", "zh_cn" }) {
            try (InputStream in = HudLayoutTest.class.getResourceAsStream("/assets/nerospace/lang/" + locale + ".json")) {
                assertNotNull(in, locale);
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                    if (e.getKey().startsWith("hud.nerospace.clock.") && !e.getKey().endsWith(".tooltip")) {
                        out.add(e.getValue().getAsString());
                    }
                }
            }
        }
        assertTrue(out.size() >= 21, "clock labels in three languages");
        // The window names are shown too: the longest label must be among those checked.
        assertTrue(out.contains("Sorgere della Terra"), "IT Earthrise");
        assertTrue(out.contains("地球升起") && out.contains("暮光"), "ZH Earthrise and Twilight");
        String longest = out.stream().max(java.util.Comparator.comparingInt(HudLayoutTest::width)).orElseThrow();
        assertTrue(width(longest) >= width("Sorgere della Terra"), "longest label " + longest);
        return out;
    }

    @Test
    @DisplayName("dial and phase label clear the five-line stack, on screen, in EN/IT/ZH, at both scales")
    void dialAndLabelNeverCollide() throws IOException {
        List<String> labels = clockLabels();
        for (int[] gui : GUIS) {
            int x = HudLayout.anchorX(gui[0]);
            int y = HudLayout.anchorY(gui[1]);
            for (int scale = 1; scale <= 2; scale++) {
                int[] dial = HudLayout.dial(x, y, scale);
                assertTrue(dial[1] >= 0 && dial[0] + dial[2] <= gui[0], "dial on screen " + gui[0] + "x" + gui[1]);
                for (int k = 0; k < HudLayout.MAX_LINES; k++) {
                    int[] line = HudLayout.line(x, y, k, gui[0]);
                    assertFalse(HudLayout.overlaps(dial, line), "dial vs line " + k + " at " + gui[0] + " scale " + scale);
                }
                for (String label : labels) {
                    int[] r = HudLayout.phaseLabel(x, y, scale, width(label), gui[0]);
                    String where = "'" + label + "' at " + gui[0] + "x" + gui[1] + " scale " + scale;
                    assertFalse(HudLayout.overlaps(r, dial), where + " overlaps the dial");
                    for (int k = 0; k < HudLayout.MAX_LINES; k++) {
                        assertFalse(HudLayout.overlaps(r, HudLayout.line(x, y, k, gui[0])), where + " overlaps line " + k);
                    }
                    assertTrue(r[0] >= 0 && r[0] + r[2] <= gui[0] && r[1] >= 0, where + " off screen");
                }
            }
        }
    }

    @Test
    @DisplayName("the five status lines never overlap each other or the oxygen bar")
    void linesStack() {
        int x = HudLayout.anchorX(427);
        int y = HudLayout.anchorY(240);
        int[] bar = { HudLayout.barX(x), HudLayout.barY(y), HudLayout.BAR_W, HudLayout.BAR_H };
        for (int k = 0; k < HudLayout.MAX_LINES; k++) {
            int[] a = HudLayout.line(x, y, k, 427);
            assertFalse(HudLayout.overlaps(a, bar), "line " + k + " vs bar");
            for (int j = k + 1; j < HudLayout.MAX_LINES; j++) {
                assertFalse(HudLayout.overlaps(a, HudLayout.line(x, y, j, 427)), k + " vs " + j);
            }
        }
    }
}
