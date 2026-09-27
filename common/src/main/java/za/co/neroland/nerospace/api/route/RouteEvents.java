package za.co.neroland.nerospace.api.route;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subscription bus for cargo-route events. <b>Public API — semver-stable.</b>
 *
 * <p>Listeners run on the server thread inside Nerospace's own tick, so they must be quick and must not
 * throw — a listener that throws is logged (class name only) and skipped for that event, never
 * unsubscribed, and never allowed to take the flight down with it. Every callback has a default no-op
 * body, so a subscriber overrides only what it cares about and new events can be added in minor
 * versions without breaking anyone. The handles passed are snapshots (see {@link FlightHandle}); no
 * player UUID travels with an event.</p>
 *
 * <p>NeroLogistics: {@link Listener#onFlightDeparted} / {@link Listener#onFlightArrived} are the departure
 * and arrival callbacks its {@code ShipmentManager} needs; {@link Listener#onFlightDropped} is the
 * "cargo ended up in a crate" case it should surface on its dashboard.</p>
 */
public final class RouteEvents {

    /** Implement the callbacks you need; the rest default to no-ops. */
    public interface Listener {

        /** A Cargo Pad was placed (or re-registered after a store recovery), or a station became routable. */
        default void onPadRegistered(PadHandle pad) {
        }

        /** A Cargo Pad was broken (its routes were removed; flights bound for it will hold, then crate). */
        default void onPadUnregistered(PadHandle pad) {
        }

        default void onFlightDeparted(FlightHandle flight) {
        }

        /** The manifest finished moving into the destination pad. */
        default void onFlightArrived(FlightHandle flight) {
        }

        /** Fired once when a flight first enters {@link FlightState#HOLDING}. */
        default void onFlightHeld(FlightHandle flight) {
        }

        /** The remaining manifest was crated at the destination. */
        default void onFlightDropped(FlightHandle flight) {
        }
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private RouteEvents() {
    }

    /** Subscribe; safe from mod construction onwards. Idempotent for the same instance. */
    public static void subscribe(Listener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static boolean unsubscribe(Listener listener) {
        return LISTENERS.remove(listener);
    }

    /** Snapshot of the current subscribers (internal dispatch reads this; consumers rarely need it). */
    public static List<Listener> listeners() {
        return List.copyOf(LISTENERS);
    }
}
