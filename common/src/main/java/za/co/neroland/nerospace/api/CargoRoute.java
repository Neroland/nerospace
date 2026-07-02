package za.co.neroland.nerospace.api;

/**
 * Read-only, immutable description of one directed cargo-rocket route between two Nerospace route
 * endpoints, with the cost model a consumer (e.g. NeroLogistics) needs to schedule a shipment.
 *
 * <p><b>Public API — semver-stable.</b> Part of {@code za.co.neroland.nerospace.api}, the only supported
 * surface for other Neroland mods. Obtain instances from {@link NerospaceRoutes#route} /
 * {@link NerospaceRoutes#routesFrom} — the facade computes the tier, fuel and transit values from
 * Nerospace's live launch model, so query a fresh route per shipment rather than caching records across
 * config reloads.</p>
 *
 * @param from              the departure endpoint
 * @param to                the arrival endpoint (always a different dimension than {@code from})
 * @param minimumRocketTier the lowest rocket tier (1–4, see {@link NerospaceRoutes#MAX_ROCKET_TIER}) able
 *                          to fly this route — the tier whose destination unlocks cover both endpoints
 * @param fuelCostMb        rocket fuel burned by one launch on this route, in millibuckets. This is the
 *                          same cross-dimension cost a crewed launch pays, already scaled by the server's
 *                          {@code fuelCost} config multiplier at query time.
 * @param transitTicks      Nerospace's canonical cargo transit duration for this route, in game ticks —
 *                          {@link NerospaceRoutes#TRANSIT_TICKS_PER_STEP} per step of separation in the
 *                          global destination order, so farther bodies take proportionally longer.
 *                          Consumers with their own pacing config may treat this as a default.
 */
public record CargoRoute(RouteEndpoint from, RouteEndpoint to, int minimumRocketTier, int fuelCostMb,
        int transitTicks) {

    public CargoRoute {
        if (from == null) {
            throw new IllegalArgumentException("CargoRoute from must not be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("CargoRoute to must not be null");
        }
        if (from.dimension().equals(to.dimension())) {
            throw new IllegalArgumentException("CargoRoute endpoints must differ: " + from.id());
        }
    }

    /** Whether this route crosses a dimension boundary. Always {@code true} — kept for readability. */
    public boolean crossDimension() {
        return true;
    }
}
