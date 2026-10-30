package za.co.neroland.nerospace.client.ui;

/**
 * HUD geometry next to the hotbar, free of renderer types so it is unit-tested. {@code (x, y)} is the oxygen icon's
 * top-left; the status lines (O2, suit, rad lining, weather, hazard: at most {@link #MAX_LINES}) stack upward from
 * the bar, and the clock dial sits on a fixed anchor above the tallest possible stack so it never jumps as lines
 * come and go. The phase label goes right of the dial, or above it when the screen edge is too close; it never
 * shares a row with a status line.
 */
public final class HudLayout {

    public static final int ICON = 16;
    public static final int BAR_W = 80;
    public static final int BAR_H = 6;
    public static final int LINE_PITCH = 10;
    public static final int LINE_H = 9;
    /** O2, suit, rad lining, weather, hazard. */
    public static final int MAX_LINES = 5;
    /** Leading glyph offset left of a line's text. */
    public static final int GLYPH_LEFT = 10;
    /** Gap between the top status line and the dial box. */
    public static final int DIAL_GAP = 2;

    private HudLayout() {
    }

    /** The anchor's x for a GUI {@code guiWidth} wide: right of the hotbar, clamped on-screen. */
    public static int anchorX(int guiWidth) {
        int totalW = ICON + 3 + BAR_W;
        return Math.min(guiWidth / 2 + 91 + 4, guiWidth - totalW - 2);
    }

    public static int anchorY(int guiHeight) {
        return guiHeight - 19;
    }

    public static int barX(int x) {
        return x + ICON + 3;
    }

    public static int barY(int y) {
        return y + ICON / 2 - BAR_H / 2;
    }

    /** Top y of status line {@code k} (0 = the O2 line just above the bar). */
    public static int lineY(int y, int k) {
        return barY(y) - LINE_PITCH * (k + 1);
    }

    /** Dial radius for config scale 1 or 2 (box 16 or 32 px). */
    public static int dialRadius(int scale) {
        return scale >= 2 ? 15 : 7;
    }

    /** The dial box {@code {x, y, size, size}}: above the top of a full five-line stack. */
    public static int[] dial(int x, int y, int scale) {
        int size = ClockDial.size(dialRadius(scale));
        return new int[] { x, lineY(y, MAX_LINES - 1) - DIAL_GAP - size, size, size };
    }

    /** The phase label rect {@code {x, y, w, 9}} for a label {@code textWidth} wide. */
    public static int[] phaseLabel(int x, int y, int scale, int textWidth, int guiWidth) {
        int[] d = dial(x, y, scale);
        int lx = d[0] + d[2] + 4;
        if (lx + textWidth <= guiWidth - 2) {
            return new int[] { lx, d[1] + (d[3] - 8) / 2, textWidth, LINE_H };
        }
        int ax = Math.max(2, Math.min(d[0], guiWidth - 2 - textWidth));
        return new int[] { ax, d[1] - LINE_H - 2, textWidth, LINE_H };
    }

    /** Rect of status line {@code k} from its leading glyph to the right edge of the screen. */
    public static int[] line(int x, int y, int k, int guiWidth) {
        int lx = barX(x) - GLYPH_LEFT;
        return new int[] { lx, lineY(y, k), guiWidth - lx, LINE_H };
    }

    public static boolean overlaps(int[] a, int[] b) {
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
    }
}
