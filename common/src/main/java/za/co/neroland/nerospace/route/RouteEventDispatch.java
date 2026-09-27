package za.co.neroland.nerospace.route;

import java.util.function.Consumer;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.route.FlightHandle;
import za.co.neroland.nerospace.api.route.PadHandle;
import za.co.neroland.nerospace.api.route.RouteEvents;

/**
 * Internal fan-out for {@link RouteEvents}: every subscriber is invoked inside its own try/catch so a
 * broken listener is logged (class name only — never a player, never a position) and skipped, and the
 * flight or pad operation that raised the event always completes.
 */
final class RouteEventDispatch {

    private RouteEventDispatch() {
    }

    static void padRegistered(PadHandle pad) {
        each(l -> l.onPadRegistered(pad), "onPadRegistered");
    }

    static void padUnregistered(PadHandle pad) {
        each(l -> l.onPadUnregistered(pad), "onPadUnregistered");
    }

    static void departed(FlightHandle flight) {
        each(l -> l.onFlightDeparted(flight), "onFlightDeparted");
    }

    static void arrived(FlightHandle flight) {
        each(l -> l.onFlightArrived(flight), "onFlightArrived");
    }

    static void held(FlightHandle flight) {
        each(l -> l.onFlightHeld(flight), "onFlightHeld");
    }

    static void dropped(FlightHandle flight) {
        each(l -> l.onFlightDropped(flight), "onFlightDropped");
    }

    private static void each(Consumer<RouteEvents.Listener> call, String event) {
        for (RouteEvents.Listener listener : RouteEvents.listeners()) {
            try {
                call.accept(listener);
            } catch (RuntimeException | LinkageError e) {
                NerospaceCommon.LOGGER.warn("[Nerospace] Route event listener {} failed on {}; skipped.",
                        listener.getClass().getName(), event, e);
            }
        }
    }
}
