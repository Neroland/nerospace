package za.co.neroland.nerospace.api.route;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

/**
 * The outcome of {@link RouteApi#requestFlight}. <b>Public API — semver-stable.</b> Either a flight was
 * created ({@link #flight()} present) or one {@link Denial} explains why not — the same reasons the pad
 * GUI shows a player, so a consumer can surface them verbatim.
 *
 * @param flight the launched flight, or empty when denied
 * @param denial why the request was refused, or {@code null} when accepted
 */
public record FlightRequestResult(Optional<FlightHandle> flight, @Nullable Denial denial) {

    /** Why a flight request was refused. New constants may be added in minor versions. */
    public enum Denial {
        /** The origin id is not a registered Cargo Pad (stations cannot launch cargo). */
        NO_ORIGIN,
        /** The destination id is not a registered pad or station, or equals the origin. */
        NO_DESTINATION,
        /** The requester may not launch from the origin or deliver to the destination. */
        NOT_PERMITTED,
        /** The origin pad's chunk is not loaded — there is no rocket to launch. */
        ORIGIN_NOT_LOADED,
        /** No cargo rocket is docked on the origin pad (or it is already lifting off). */
        NO_ROCKET,
        /** The pad formation under the rocket is below the required tier (a 3×3 pad). */
        PAD_TOO_SMALL,
        /** The manifest is empty or exceeds the server's cargo slot cap. */
        BAD_MANIFEST,
        /** The docked rocket holds less fuel than the quote. */
        NOT_ENOUGH_FUEL,
        /** The requester already has the maximum number of live flights. */
        TOO_MANY_FLIGHTS
    }

    public static FlightRequestResult accepted(FlightHandle flight) {
        return new FlightRequestResult(Optional.of(flight), null);
    }

    public static FlightRequestResult denied(Denial denial) {
        return new FlightRequestResult(Optional.empty(), denial);
    }

    public boolean accepted() {
        return this.flight.isPresent();
    }
}
