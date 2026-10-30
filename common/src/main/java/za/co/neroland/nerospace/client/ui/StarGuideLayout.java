package za.co.neroland.nerospace.client.ui;

/**
 * Star Guide geometry (panel-relative px at GUI scale 1, panel 240x200), kept free of renderer types so it is
 * unit-tested: a scrolling chapter rail of {@link #RAIL_VISIBLE} entries, a serpentine step canvas three nodes
 * wide, and a scrolling text well. Rects are {@code {x, y, w, h}}.
 */
public final class StarGuideLayout {

    public static final int PANEL_W = 240;
    public static final int PANEL_H = 200;

    /** Rail well (in the PNG) and its entries. */
    public static final int[] RAIL_WELL = { 6, 19, 88, 175 };
    public static final int RAIL_X = 10;
    public static final int RAIL_Y = 22;
    public static final int RAIL_PITCH = 16;
    public static final int RAIL_W = 80;
    public static final int RAIL_H = 14;
    public static final int RAIL_VISIBLE = 10;
    public static final int[] RAIL_BAR = { 91, 22, 2, 160 };
    public static final int[] CHEV_UP = { 10, 184, 28, 8 };
    public static final int[] CHEV_DOWN = { 62, 184, 28, 8 };

    /** Step canvas well (in the PNG) and node geometry. */
    public static final int[] STEP_WELL = { 96, 19, 138, 76 };
    public static final int[] NODE_COLS = { 100, 146, 192 };
    public static final int NODE_Y = 22;
    public static final int NODE_W = 40;
    public static final int NODE_H = 14;
    public static final int NODE_PITCH = 22;
    public static final int PROGRESS_RIGHT = 230;
    public static final int PROGRESS_Y = 84;

    /** Text well (in the PNG), step title and description. */
    public static final int[] TEXT_WELL = { 96, 97, 138, 97 };
    public static final int TITLE_X = 100;
    public static final int TITLE_Y = 100;
    public static final int TITLE_W = 130;
    public static final int TITLE_W_DONE = 120;
    public static final int DONE_MARK_X = 222;
    public static final int TEXT_X = 100;
    public static final int TEXT_Y = 112;
    public static final int TEXT_W = 130;
    public static final int LINE_H = 9;
    public static final int TEXT_LINES = 9;
    public static final int[] TEXT_BAR = { 231, 112, 2, 81 };

    private StarGuideLayout() {
    }

    /** Largest rail scroll for {@code chapters} chapters. */
    public static int maxRailScroll(int chapters) {
        return Math.max(0, chapters - RAIL_VISIBLE);
    }

    /** The rail entry rect for visible row {@code row} (0..9). */
    public static int[] railEntry(int row) {
        return new int[] { RAIL_X, RAIL_Y + RAIL_PITCH * row, RAIL_W, RAIL_H };
    }

    /** The rail scroll that keeps {@code chapter} visible, moving as little as possible from {@code scroll}. */
    public static int scrollToShow(int chapter, int scroll, int chapters) {
        int s = scroll;
        if (chapter < s) {
            s = chapter;
        } else if (chapter >= s + RAIL_VISIBLE) {
            s = chapter - RAIL_VISIBLE + 1;
        }
        return Math.max(0, Math.min(maxRailScroll(chapters), s));
    }

    /** Rail scrollbar thumb {@code {y, h}} (absolute panel y), or null when every chapter fits. */
    public static int[] railThumb(int scroll, int chapters) {
        if (chapters <= RAIL_VISIBLE) {
            return null;
        }
        int h = Math.max(8, RAIL_BAR[3] * RAIL_VISIBLE / chapters);
        int max = maxRailScroll(chapters);
        int y = RAIL_BAR[1] + (RAIL_BAR[3] - h) * Math.max(0, Math.min(max, scroll)) / max;
        return new int[] { y, h };
    }

    /** Vertical pitch between node rows for {@code nodes} shown nodes (compressed past three rows). */
    public static int nodePitch(int nodes) {
        int rows = Math.max(1, (nodes + 2) / 3);
        return rows <= 3 ? NODE_PITCH : Math.max(16, 62 / (rows - 1));
    }

    /** Node rect for slot {@code slot} of {@code nodes}: serpentine, odd rows run right to left. */
    public static int[] node(int slot, int nodes) {
        int row = slot / 3;
        int col = slot % 3;
        if (row % 2 == 1) {
            col = 2 - col;
        }
        return new int[] { NODE_COLS[col], NODE_Y + row * nodePitch(nodes), NODE_W, NODE_H };
    }

    /** Largest description scroll for {@code lines} wrapped lines. */
    public static int maxTextScroll(int lines) {
        return Math.max(0, lines - TEXT_LINES);
    }

    /** Text scrollbar thumb {@code {y, h}}, or null when the text fits. */
    public static int[] textThumb(int scroll, int lines) {
        if (lines <= TEXT_LINES) {
            return null;
        }
        int h = Math.max(8, TEXT_BAR[3] * TEXT_LINES / lines);
        int max = maxTextScroll(lines);
        int y = TEXT_BAR[1] + (TEXT_BAR[3] - h) * Math.max(0, Math.min(max, scroll)) / max;
        return new int[] { y, h };
    }

    public static boolean inside(int[] rect, double x, double y) {
        return x >= rect[0] && x < rect[0] + rect[2] && y >= rect[1] && y < rect[1] + rect[3];
    }
}
