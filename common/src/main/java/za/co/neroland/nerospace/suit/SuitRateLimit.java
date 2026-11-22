package za.co.neroland.nerospace.suit;

/**
 * Drops excess packets. The counter is a plain int: no player id is stored in the metric.
 */
public final class SuitRateLimit {

    private int windowStart;
    private int inWindow;
    private int dropped;

    public SuitRateLimit() {
    }

    /**
     * @param now   a monotonic tick
     * @param limit packets allowed per 20-tick second
     * @return true when the packet may be handled
     */
    public boolean allow(int now, int limit) {
        if (now - this.windowStart >= 20) {
            this.windowStart = now;
            this.inWindow = 0;
        }
        if (this.inWindow >= limit) {
            this.dropped++;
            return false;
        }
        this.inWindow++;
        return true;
    }

    public int dropped() {
        return this.dropped;
    }
}
