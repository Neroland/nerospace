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
    /**
     * Whether a loaded rocket follows the 2.0 charter rule. A 2.0 save carries {@code CharterRule}; a 1.6.0 save
     * has none, and only a rocket that was already in flight ({@code Launching}) is honoured once — a parked 1.6.0
     * rocket with "New Station" still selected follows the 2.0 rule (D6: one charter per station).
     *
     * @param savedRule the saved {@code CharterRule}, or {@code null} when the field is absent
     * @param launching the saved {@code Launching} flag
     */
    public static boolean charterRuleAware(@org.jetbrains.annotations.Nullable Boolean savedRule, boolean launching) {
        return savedRule != null ? savedRule.booleanValue() : !launching;
    }

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
