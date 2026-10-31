package za.co.neroland.nerospace.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * The rocket console's destination list as pure index arithmetic: which {@code Destinations} index each list
 * entry stands for, which entry a visible row shows at a scroll offset, and how far to scroll to keep the
 * selection in view. The screen draws and hit-tests the rows; everything that maps a row to an index lives here.
 */
public final class DestinationList {

    private DestinationList() {
    }

    /**
     * The list entries in display order: every index in {@code mask} plus the "you are here" index (both in
     * {@code Destinations} order, never twice), then the Observatory previews in the order given.
     */
    public static List<Integer> entries(int mask, int here, int count, List<Integer> previews) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if ((mask & (1 << i)) != 0 || i == here) {
                out.add(i);
            }
        }
        for (Integer preview : previews) {
            if (!out.contains(preview)) {
                out.add(preview);
            }
        }
        return out;
    }

    /** The scroll offset clamped so the visible window never runs past the end of the list. */
    public static int clampScroll(int scroll, int size, int visible) {
        return Math.max(0, Math.min(scroll, Math.max(0, size - visible)));
    }

    /** The {@code Destinations} index shown in visible {@code row} at {@code scroll}, or −1 for an empty row. */
    public static int indexAt(List<Integer> entries, int scroll, int row, int visible) {
        if (row < 0 || row >= visible) {
            return -1;
        }
        int i = clampScroll(scroll, entries.size(), visible) + row;
        return i >= 0 && i < entries.size() ? entries.get(i) : -1;
    }

    /** The scroll offset that brings {@code index} into view (unchanged when it is already visible or absent). */
    public static int scrollToShow(List<Integer> entries, int scroll, int index, int visible) {
        int at = entries.indexOf(index);
        if (at < 0) {
            return scroll;
        }
        if (at < scroll) {
            return at;
        }
        if (at >= scroll + visible) {
            return at - visible + 1;
        }
        return scroll;
    }
}
