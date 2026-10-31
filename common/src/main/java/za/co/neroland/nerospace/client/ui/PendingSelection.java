package za.co.neroland.nerospace.client.ui;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.rocket.DestinationSelection;
import za.co.neroland.nerospace.rocket.DestinationSelection.Outcome;

/**
 * The optimistic destination highlight: a click shows its pick at once, and the synced menu values — the
 * server's decision — settle it. Pure; the host screen feeds it the synced index, the synced outcome code
 * ({@link DestinationSelection#encodeOutcome}) and the clock every frame.
 *
 * <p>A refusal is reported with the server's own reason ({@link #refusal()}); only a pick the server never
 * answered within {@link #TIMEOUT_MS} is a refusal without a known reason.</p>
 */
public final class PendingSelection {

    /** How long a pick may wait for any server answer before the screen treats it as refused. */
    public static final long TIMEOUT_MS = 3000L;

    /** {@link #begin(int, int, long)} without an outcome code: only the synced index settles the pick. */
    private static final int IGNORE_OUTCOME = -1;

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
    private int baseOutcome = IGNORE_OUTCOME;
    private long since;
    @Nullable
    private Outcome refusal;

    /** Starts showing {@code index} while the synced value is still {@code synced}. */
    public void begin(int index, int synced, long nowMs) {
        begin(index, synced, IGNORE_OUTCOME, nowMs);
    }

    /**
     * Starts showing {@code index} while the synced value is still {@code synced} and the synced outcome code is
     * still {@code outcomeCode}: the next different code is the server's answer to this pick.
     */
    public void begin(int index, int synced, int outcomeCode, long nowMs) {
        this.pending = index;
        this.base = synced;
        this.baseOutcome = outcomeCode;
        this.since = nowMs;
        this.refusal = null;
    }

    /** Settles the pick against the latest synced index (no outcome code). */
    public Result reconcile(int synced, long nowMs) {
        return reconcile(synced, IGNORE_OUTCOME, nowMs);
    }

    /** Settles the pick against the latest synced index and outcome code. */
    public Result reconcile(int synced, int outcomeCode, long nowMs) {
        if (this.pending < 0) {
            return Result.NONE;
        }
        if (synced == this.pending) {
            clear();
            return Result.CONFIRMED;
        }
        if (this.baseOutcome != IGNORE_OUTCOME && outcomeCode != IGNORE_OUTCOME && outcomeCode != this.baseOutcome) {
            Outcome answer = DestinationSelection.decodeOutcome(outcomeCode);
            if (answer != null && !answer.accepted()) {
                return refuse(answer);
            }
            if (answer == Outcome.ALREADY_SELECTED && synced == this.base) {
                return refuse(answer); // a step that found nowhere else to go
            }
            // Accepted: the synced index settles it (it is sent before the outcome, so normally already here).
        }
        if (synced != this.base) {
            clear();
            return Result.OVERRIDDEN;
        }
        if (nowMs - this.since >= TIMEOUT_MS) {
            return refuse(null);
        }
        return Result.NONE;
    }

    private Result refuse(@Nullable Outcome reason) {
        clear();
        this.refusal = reason;
        return Result.REFUSED;
    }

    /**
     * The server's reason for the last {@link Result#REFUSED}, or {@code null} when the server never answered
     * (timeout) and the reason is unknown.
     */
    @Nullable
    public Outcome refusal() {
        return this.refusal;
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
        this.baseOutcome = IGNORE_OUTCOME;
    }
}
