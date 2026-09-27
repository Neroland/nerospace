package za.co.neroland.nerospace.api.route;

/**
 * The lifecycle of a cargo flight. <b>Public API — semver-stable</b>; new constants may be added in minor
 * versions, so consumers should treat unknown states as "still live" unless {@link #isTerminal()}.
 */
public enum FlightState {

    /** Launched; the server-side timer is running. Nothing is loaded or rendered in transit. */
    IN_FLIGHT,
    /**
     * The timer has elapsed but the destination dimension or chunk is not loaded. Arrival is deferred
     * until it loads (or a player brings it into range); Nerospace never loads anything to deliver. Does
     * not count towards the holding timeout.
     */
    AWAITING_CHUNK,
    /**
     * The destination is loaded but cannot take the rocket right now (no Cargo Pad at the position, a
     * rocket already docked, or the pad refused every stack). Retried on the configured interval; after
     * the configured timeout the cargo is crated at the destination ({@link #DROPPED}).
     */
    HOLDING,
    /** Materialised on the destination pad; the manifest is moving into the pad inventory over a few ticks. */
    UNLOADING,
    /** Everything was delivered into the destination pad. Terminal. */
    DELIVERED,
    /**
     * The flight could not be delivered in time (or was cancelled while holding): the remaining manifest
     * was dropped as a Cargo Crate item at the destination position. Nothing was deleted. Terminal.
     */
    DROPPED;

    /** Whether the flight is finished (no further state changes). */
    public boolean isTerminal() {
        return this == DELIVERED || this == DROPPED;
    }

    /** Safe lookup by name for persistence; unknown names read as {@link #IN_FLIGHT}. */
    public static FlightState byName(String name) {
        if (name == null) {
            return IN_FLIGHT;
        }
        for (FlightState state : values()) {
            if (state.name().equalsIgnoreCase(name)) {
                return state;
            }
        }
        return IN_FLIGHT;
    }
}
