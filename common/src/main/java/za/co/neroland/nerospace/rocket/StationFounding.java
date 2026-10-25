package za.co.neroland.nerospace.rocket;

/**
 * Pure "New Station from the rocket" rule (001 §13.2 / §18.2, 005 §8): a charter is consumed on arrival; a
 * rocket saved by 1.6.0 already in flight with {@code StationSlot:-2} is honoured once for free; no charter →
 * the origin platform with a message; registry full → nothing founded, nothing consumed.
 */
public final class StationFounding {

    public enum Outcome { FOUND_FREE, FOUND_CONSUME, REDIRECT, REDIRECT_FULL }

    private StationFounding() {
    }

    /**
     * @param legacy       the rocket was saved before the charter rule (no {@code CharterRule} flag)
     * @param hasCharter   the rider carries at least one Station Charter
     * @param registryFull {@code StationRegistry} is at {@code MAX_STATIONS}
     */
    public static Outcome resolve(boolean legacy, boolean hasCharter, boolean registryFull) {
        if (!legacy && !hasCharter) {
            return Outcome.REDIRECT;
        }
        if (registryFull) {
            return Outcome.REDIRECT_FULL;
        }
        return legacy ? Outcome.FOUND_FREE : Outcome.FOUND_CONSUME;
    }
}
