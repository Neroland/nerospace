package za.co.neroland.nerospace.client.ui;

/**
 * The optimistic destination highlight: a click shows its pick at once, and the synced menu value — the
 * server's decision — settles it. Pure; the host screen feeds it the synced index and the clock every frame.
 */
public final class PendingSelection {

    /** How long a pick may wait for the server before the screen treats it as refused. */
    public static final long TIMEOUT_MS = 1500L;

    /** What {@link #reconcile} concluded this frame. */
    public enum Result {
        /** Nothing pending, or still waiting. */
        NONE,
        /** The server applied the pick. */
        CONFIRMED,
        /** The server settled on a different destination (e.g. skipped a gate-locked world). */
        OVERRIDDEN,
        /** The server kept the old destination: the pick was refused. */
        REFUSED
    }

    private int pending = -1;
    private int base = -1;
    private long since;

    /** Starts showing {@code index} while the synced value is still {@code synced}. */
    public void begin(int index, int synced, long nowMs) {
        this.pending = index;
        this.base = synced;
        this.since = nowMs;
    }

    /** Settles the pick against the latest synced value. */
    public Result reconcile(int synced, long nowMs) {
        if (this.pending < 0) {
            return Result.NONE;
        }
        if (synced == this.pending) {
            clear();
            return Result.CONFIRMED;
        }
        if (synced != this.base) {
            clear();
            return Result.OVERRIDDEN;
        }
        if (nowMs - this.since >= TIMEOUT_MS) {
            clear();
            return Result.REFUSED;
        }
        return Result.NONE;
    }

    /** The index the screen should show as selected. */
    public int shown(int synced) {
        return this.pending >= 0 ? this.pending : synced;
    }

    public boolean active() {
        return this.pending >= 0;
    }

    public void clear() {
        this.pending = -1;
        this.base = -1;
    }
}
