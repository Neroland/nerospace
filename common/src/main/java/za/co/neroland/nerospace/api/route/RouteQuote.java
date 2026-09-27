package za.co.neroland.nerospace.api.route;

/**
 * What a shipment between two pads would cost, computed from the planet registry at call time (see
 * {@code docs/CARGO-ROCKETS.md} §4). <b>Public API — semver-stable.</b> Query per shipment: both values
 * follow the server's live config multipliers.
 *
 * @param travelTicks    outbound flight time in ticks (one minute = 1200)
 * @param fuelMb         rocket fuel the origin pad must hold for the trip — including the empty return
 *                       leg when {@code returnEmpty} was requested
 * @param crossDimension whether the two pads are in different dimensions
 * @param massUnits      the payload's mass in 64-item stack-equivalents (what the fuel formula saw)
 */
public record RouteQuote(int travelTicks, int fuelMb, boolean crossDimension, int massUnits) {

    /** Travel time rounded up to whole seconds, for display. */
    public int travelSeconds() {
        return (this.travelTicks + 19) / 20;
    }
}
