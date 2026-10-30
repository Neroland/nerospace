package za.co.neroland.nerospace.client.ui;

/**
 * Station Console geometry (panel-relative px at GUI scale 1, panel 240x200), free of renderer types so it is
 * unit-tested: header name and badge, the Overview module list on a 9 px pitch, the shared button row, one Build
 * row per module and the Docking pad cards. Rects are {@code {x, y, w, h}}.
 */
public final class ConsoleLayout {

    public static final int PANEL_W = 240;
    public static final int PANEL_H = 200;
    /** Page well (in the PNG). */
    public static final int[] PAGE_WELL = { 6, 36, 228, 158 };

    public static final int NAME_X = 58;
    public static final int NAME_MAX = 110;
    public static final int BADGE_RIGHT = 232;
    public static final int BADGE_MAX = 100;

    public static final int MODULE_HEADER_Y = 100;
    public static final int MODULE_ROW_Y = 110;
    public static final int MODULE_PITCH = 9;
    public static final int MODULE_ROWS = 7;
    /** Rename / Abandon / Claim (Overview), Add member, Send visitors home. */
    public static final int BUTTON_ROW_Y = 180;

    public static final int BUILD_ROW_Y = 52;
    public static final int BUILD_PITCH = 18;
    public static final int BUILD_NAME_W = 86;
    public static final int BUILD_STATE_DX = 104;
    public static final int BUILD_STATE_W = 58;
    public static final int BUILD_STATE_W_TOGGLE = 14;
    public static final int TOGGLE_BUTTON_DX = 120;
    public static final int TOGGLE_BUTTON_W = 42;
    /** Wide enough for the longest shipped label ("Costruisci", 50 px) without an ellipsis. */
    public static final int BUILD_BUTTON_DX = 164;
    public static final int BUILD_BUTTON_W = 54;
    public static final int BUILD_RESULT_Y = 184;

    public static final int DOCK_CARDS = 6;

    private ConsoleLayout() {
    }

    /** Top y of Overview module row {@code tile} (0 = core). */
    public static int moduleRowY(int tile) {
        return MODULE_ROW_Y + MODULE_PITCH * tile;
    }

    /** The Build tab row for module {@code tile} (0 = core). */
    public static int[] buildRow(int tile) {
        return new int[] { 10, BUILD_ROW_Y + BUILD_PITCH * tile, 220, 16 };
    }

    /** Docking compact pad card {@code i} (0..5): two rows of three. */
    public static int[] dockCard(int i) {
        return new int[] { 12 + 68 * (i % 3), 78 + 34 * (i / 3), 64, 32 };
    }
}
