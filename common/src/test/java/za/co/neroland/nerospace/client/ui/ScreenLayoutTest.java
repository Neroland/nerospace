package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.progression.StarGuide;

/** Star Guide and Station Console geometry: everything inside its well, nothing overlapping. */
class ScreenLayoutTest {

    private static boolean inside(int[] inner, int[] outer) {
        return inner[0] >= outer[0] && inner[1] >= outer[1] && inner[0] + inner[2] <= outer[0] + outer[2]
                && inner[1] + inner[3] <= outer[1] + outer[3];
    }

    private static boolean overlap(int[] a, int[] b) {
        return HudLayout.overlaps(a, b);
    }

    private static void assertNoOverlap(List<int[]> rects, String what) {
        for (int i = 0; i < rects.size(); i++) {
            for (int j = i + 1; j < rects.size(); j++) {
                assertFalse(overlap(rects.get(i), rects.get(j)), what + " " + i + " overlaps " + j);
            }
        }
    }

    @Test
    @DisplayName("Star Guide rail: ten entries and both chevrons inside the rail well, none overlapping")
    void starGuideRail() {
        List<int[]> rects = new ArrayList<>();
        for (int row = 0; row < StarGuideLayout.RAIL_VISIBLE; row++) {
            int[] r = StarGuideLayout.railEntry(row);
            assertTrue(inside(r, StarGuideLayout.RAIL_WELL), "rail entry " + row);
            assertFalse(overlap(r, StarGuideLayout.RAIL_BAR), "rail entry " + row + " under the scrollbar");
            rects.add(r);
        }
        assertTrue(inside(StarGuideLayout.CHEV_UP, StarGuideLayout.RAIL_WELL));
        assertTrue(inside(StarGuideLayout.CHEV_DOWN, StarGuideLayout.RAIL_WELL));
        assertTrue(inside(StarGuideLayout.RAIL_BAR, StarGuideLayout.RAIL_WELL));
        rects.add(StarGuideLayout.CHEV_UP);
        rects.add(StarGuideLayout.CHEV_DOWN);
        assertNoOverlap(rects, "rail");
    }

    @Test
    @DisplayName("Star Guide rail scrolls over every chapter and keeps the selection in view")
    void starGuideScroll() {
        int n = StarGuide.CHAPTER_COUNT;
        assertEquals(Math.max(0, n - 10), StarGuideLayout.maxRailScroll(n));
        for (int c = 0; c < n; c++) {
            int s = StarGuideLayout.scrollToShow(c, 0, n);
            assertTrue(c >= s && c < s + StarGuideLayout.RAIL_VISIBLE, "chapter " + c + " visible");
            s = StarGuideLayout.scrollToShow(c, StarGuideLayout.maxRailScroll(n), n);
            assertTrue(c >= s && c < s + StarGuideLayout.RAIL_VISIBLE, "chapter " + c + " visible from the end");
        }
        assertNull(StarGuideLayout.railThumb(0, 10));
        if (n > 10) {
            for (int s = 0; s <= StarGuideLayout.maxRailScroll(n); s++) {
                int[] t = StarGuideLayout.railThumb(s, n);
                assertNotNull(t);
                int[] bar = StarGuideLayout.RAIL_BAR;
                assertTrue(t[0] >= bar[1] && t[0] + t[1] <= bar[1] + bar[3], "rail thumb at scroll " + s);
            }
        }
    }

    @Test
    @DisplayName("Star Guide nodes stay inside the step canvas and above the progress counter, for every chapter")
    void starGuideNodes() {
        for (StarGuide.Chapter chapter : StarGuide.CHAPTERS) {
            int nodes = chapter.steps().size(); // every step shown (hidden bonus done)
            List<int[]> rects = new ArrayList<>();
            for (int slot = 0; slot < nodes; slot++) {
                int[] r = StarGuideLayout.node(slot, nodes);
                assertTrue(inside(r, StarGuideLayout.STEP_WELL), chapter.id() + " node " + slot);
                assertTrue(r[1] + r[3] <= StarGuideLayout.PROGRESS_Y, chapter.id() + " node " + slot + " above progress");
                rects.add(r);
            }
            assertNoOverlap(rects, chapter.id());
        }
        // Long chapters compress but never overlap (up to the 16-step bitmask limit).
        for (int nodes = 1; nodes <= 12; nodes++) {
            List<int[]> rects = new ArrayList<>();
            for (int slot = 0; slot < nodes; slot++) {
                rects.add(StarGuideLayout.node(slot, nodes));
            }
            assertNoOverlap(rects, nodes + " nodes");
        }
    }

    @Test
    @DisplayName("Star Guide text: nine description lines and the scrollbar fit the text well")
    void starGuideText() {
        int[] well = StarGuideLayout.TEXT_WELL;
        int[] title = { StarGuideLayout.TITLE_X, StarGuideLayout.TITLE_Y, StarGuideLayout.TITLE_W, 9 };
        assertTrue(inside(title, well));
        int[] text = { StarGuideLayout.TEXT_X, StarGuideLayout.TEXT_Y, StarGuideLayout.TEXT_W,
            StarGuideLayout.TEXT_LINES * StarGuideLayout.LINE_H };
        assertTrue(inside(text, well), "description block inside the well");
        assertFalse(overlap(title, text));
        assertTrue(inside(StarGuideLayout.TEXT_BAR, well));
        assertFalse(overlap(text, StarGuideLayout.TEXT_BAR), "text clear of its scrollbar");
        assertNull(StarGuideLayout.textThumb(0, 9));
        for (int lines : new int[] { 10, 14, 24, 40 }) {
            for (int s = 0; s <= StarGuideLayout.maxTextScroll(lines); s++) {
                int[] t = StarGuideLayout.textThumb(s, lines);
                assertTrue(t[0] >= StarGuideLayout.TEXT_BAR[1]
                        && t[0] + t[1] <= StarGuideLayout.TEXT_BAR[1] + StarGuideLayout.TEXT_BAR[3]);
            }
        }
    }

    @Test
    @DisplayName("Station Console: module rows on a 9 px pitch, inside the page, above the button row")
    void consoleModuleRows() {
        List<int[]> rows = new ArrayList<>();
        int[] header = { 10, ConsoleLayout.MODULE_HEADER_Y, 96, 9 };
        rows.add(header);
        for (int t = 0; t < ConsoleLayout.MODULE_ROWS; t++) {
            int[] r = { 10, ConsoleLayout.moduleRowY(t), 96, 9 };
            assertTrue(inside(r, ConsoleLayout.PAGE_WELL), "module row " + t);
            assertTrue(r[1] + r[3] <= ConsoleLayout.BUTTON_ROW_Y, "module row " + t + " above the buttons");
            rows.add(r);
        }
        assertNoOverlap(rows, "module rows");
    }

    @Test
    @DisplayName("Station Console Build: one row per module, buttons and the cost column never collide")
    void consoleBuildRows() {
        List<int[]> rows = new ArrayList<>();
        for (int t = 0; t < ConsoleLayout.MODULE_ROWS; t++) {
            int[] r = ConsoleLayout.buildRow(t);
            assertTrue(inside(r, ConsoleLayout.PAGE_WELL), "build row " + t);
            assertTrue(r[1] + r[3] <= ConsoleLayout.BUILD_RESULT_Y, "build row " + t + " above the result line");
            int[] build = { r[0] + ConsoleLayout.BUILD_BUTTON_DX, r[1] + 2, ConsoleLayout.BUILD_BUTTON_W, 12 };
            int[] toggle = { r[0] + ConsoleLayout.TOGGLE_BUTTON_DX, r[1] + 2, ConsoleLayout.TOGGLE_BUTTON_W, 12 };
            int[] cost = { r[0] + ConsoleLayout.BUILD_STATE_DX, r[1] + 4, ConsoleLayout.BUILD_STATE_W, 9 };
            int[] costShort = { r[0] + ConsoleLayout.BUILD_STATE_DX, r[1] + 4, ConsoleLayout.BUILD_STATE_W_TOGGLE, 9 };
            int[] name = { r[0] + 16, r[1] + 4, ConsoleLayout.BUILD_NAME_W, 9 };
            assertTrue(inside(build, r) && inside(toggle, r) && inside(cost, r) && inside(name, r));
            assertFalse(overlap(build, cost), "build button vs cost");
            assertFalse(overlap(build, toggle), "build vs toggle");
            assertFalse(overlap(toggle, costShort), "toggle vs short state column");
            assertFalse(overlap(name, cost), "name vs cost");
            rows.add(r);
        }
        assertNoOverlap(rows, "build rows");
    }

    @Test
    @DisplayName("Station Console Docking: six pad cards inside the page, apart, above the routes line")
    void consoleDockCards() {
        List<int[]> cards = new ArrayList<>();
        for (int i = 0; i < ConsoleLayout.DOCK_CARDS; i++) {
            int[] c = ConsoleLayout.dockCard(i);
            int[] withBorder = { c[0] - 1, c[1] - 1, c[2] + 2, c[3] + 2 };
            assertTrue(inside(withBorder, ConsoleLayout.PAGE_WELL), "card " + i);
            assertTrue(withBorder[1] + withBorder[3] <= 154, "card " + i + " above the routes line");
            assertTrue(withBorder[1] >= 66 + 9, "card " + i + " below the paired count");
            cards.add(withBorder);
        }
        assertNoOverlap(cards, "dock cards");
    }
}
