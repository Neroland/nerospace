package za.co.neroland.nerospace.client.ui;

/**
 * Hit boxes and amounts for the oxygen HUD. Pure geometry so the hover rule (what it is, and how much)
 * can be tested without a client. Rectangles are {@code {x, y, w, h}}.
 */
public final class HudHover {

    /** Four 4 px cells with a 1 px gap. */
    public static final int WEATHER_CELLS_W = 19;
    public static final int WEATHER_CELLS_H = 6;

    private HudHover() {
    }

    public static boolean contains(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public static boolean contains(int[] rect, double mx, double my) {
        return rect != null && rect.length >= 4 && contains(rect[0], rect[1], rect[2], rect[3], mx, my);
    }

    /** Icon, oxygen line and bar, from the HUD anchor (the icon's top-left). */
    public static int[] oxygenReadout(int anchorX, int anchorY) {
        int top = HudLayout.lineY(anchorY, 0);
        int bottom = HudLayout.barY(anchorY) + HudLayout.BAR_H + 1;
        int right = HudLayout.barX(anchorX) + HudLayout.BAR_W + 1;
        return new int[] { anchorX, top, right - anchorX, bottom - top };
    }

    /** The four weather-intensity cells on a status line. {@code cellX} is the first cell's left edge. */
    public static int[] weatherCells(int cellX, int lineY) {
        return new int[] { cellX, lineY + 1, WEATHER_CELLS_W, WEATHER_CELLS_H };
    }

    /** Whole percent of {@code amount / capacity}, 0 when the capacity is not positive. */
    public static int percent(int amount, int capacity) {
        if (capacity <= 0) {
            return 0;
        }
        int clamped = Math.max(0, Math.min(capacity, amount));
        return (int) Math.round(clamped * 100.0D / capacity);
    }

    /** Weather intensity 0..1 as a whole percent. */
    public static int weatherPercent(float intensity) {
        if (!Float.isFinite(intensity)) {
            return 0;
        }
        float clamped = Math.max(0.0F, Math.min(1.0F, intensity));
        return Math.round(clamped * 100.0F);
    }

    /** Window pixels to GUI pixels. A non-positive screen size yields NaN so the caller skips the hover. */
    public static double toGui(double raw, int screenPixels, int guiPixels) {
        if (screenPixels <= 0 || !Double.isFinite(raw)) {
            return Double.NaN;
        }
        return raw * guiPixels / screenPixels;
    }
}
