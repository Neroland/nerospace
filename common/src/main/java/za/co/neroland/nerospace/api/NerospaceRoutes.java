package za.co.neroland.nerospace.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.rocket.RocketTier;
import za.co.neroland.nerospace.rocket.RocketTravel;

/**
 * Public, read-only facade over Nerospace's cargo-rocket route catalog: which dimensions rockets can
 * carry cargo between, what a launch costs in fuel, and how long a shipment is in transit.
 *
 * <p><b>Public API — semver-stable.</b> This is the static lookup entry point for consumers such as
 * NeroLogistics' {@code RouteProvider} seam: enumerate {@link #endpoints()}, build directed
 * {@link CargoRoute}s via {@link #route}/{@link #routesFrom}, and gate live shipping on {@link #isOpen}.
 * Everything outside {@code za.co.neroland.nerospace.api} (e.g. {@code rocket.Destinations},
 * {@code rocket.RocketTravel}) is internal and may change without notice.</p>
 *
 * <p><b>Design.</b> A thin facade: it holds no state and derives everything from the internal launch
 * model — endpoint order and display names from the global destination catalog, fuel from the same
 * cross-dimension launch cost a crewed rocket pays (config-scaled at query time), and the minimum rocket
 * tier from the tier destination unlocks. All returned collections and records are immutable. Routes are
 * directed dimension pairs; per-station addressing within the orbital void stays with
 * {@link NerospaceStations}.</p>
 *
 * <p><b>Privacy (POPIA/GDPR).</b> This facade exposes no player data at all — endpoints, costs and
 * durations only.</p>
 */
public final class NerospaceRoutes {

    /**
     * Canonical cargo transit time per step of separation in the global destination order
     * ({@link #endpoints()}), in game ticks. A route's {@link CargoRoute#transitTicks()} is this value
     * times the (at least one) number of steps between its endpoints — one minute per step at 20 TPS.
     */
    public static final int TRANSIT_TICKS_PER_STEP = 1_200;

    /** The highest rocket tier, i.e. the upper bound of {@link CargoRoute#minimumRocketTier()}. */
    public static final int MAX_ROCKET_TIER = 4;

    private NerospaceRoutes() {
    }

    /**
     * Every cargo-route endpoint in the stable global destination order: Home (Overworld), Orbital
     * Station, Greenxertz, Cindara, Glacira. The returned list and its elements are immutable. This is
     * static catalog data — it does not depend on which dimensions are currently loaded; use
     * {@link #isOpen} for liveness.
     */
    public static List<RouteEndpoint> endpoints() {
        List<RouteEndpoint> out = new ArrayList<>();
        for (ResourceKey<Level> key : Destinations.all()) {
            out.add(new RouteEndpoint(key, Destinations.name(key)));
        }
        return List.copyOf(out);
    }

    /** The endpoint for a dimension key, or empty if that dimension is not a cargo-route endpoint. */
    public static Optional<RouteEndpoint> endpoint(ResourceKey<Level> dimension) {
        if (dimension == null || Destinations.indexOf(dimension) < 0) {
            return Optional.empty();
        }
        return Optional.of(new RouteEndpoint(dimension, Destinations.name(dimension)));
    }

    /** Whether {@code dimension} is a cargo-route endpoint (the Overworld or a Nerospace body). */
    public static boolean isEndpoint(ResourceKey<Level> dimension) {
        return dimension != null && Destinations.indexOf(dimension) >= 0;
    }

    /**
     * The directed cargo route from {@code from} to {@code to}, or empty when either dimension is not a
     * route endpoint or both are the same dimension. Fuel cost and transit time are computed at call
     * time (fuel reflects the current {@code fuelCost} config multiplier), so query per shipment.
     */
    public static Optional<CargoRoute> route(ResourceKey<Level> from, ResourceKey<Level> to) {
        if (from == null || to == null || from.equals(to)) {
            return Optional.empty();
        }
        int fromIndex = Destinations.indexOf(from);
        int toIndex = Destinations.indexOf(to);
        if (fromIndex < 0 || toIndex < 0) {
            return Optional.empty();
        }
        return Optional.of(new CargoRoute(
                new RouteEndpoint(from, Destinations.name(from)),
                new RouteEndpoint(to, Destinations.name(to)),
                Math.max(minimumTier(from), minimumTier(to)),
                RocketTravel.cost(true, 0),
                TRANSIT_TICKS_PER_STEP * Math.max(1, Math.abs(fromIndex - toIndex))));
    }

    /**
     * All directed routes departing {@code from}, in the stable {@link #endpoints()} order (excluding
     * {@code from} itself). Empty when {@code from} is not a route endpoint. The returned list is
     * immutable.
     */
    public static List<CargoRoute> routesFrom(ResourceKey<Level> from) {
        if (from == null || Destinations.indexOf(from) < 0) {
            return List.of();
        }
        List<CargoRoute> out = new ArrayList<>();
        for (ResourceKey<Level> to : Destinations.all()) {
            route(from, to).ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    /**
     * Whether the route is currently flyable on this server: both endpoint dimensions resolve to loaded
     * {@code ServerLevel}s. Server-side; a null server yields {@code false}.
     */
    public static boolean isOpen(MinecraftServer server, CargoRoute route) {
        return server != null && route != null
                && server.getLevel(route.from().dimension()) != null
                && server.getLevel(route.to().dimension()) != null;
    }

    /**
     * Convenience for {@link #isOpen(MinecraftServer, CargoRoute)} by dimension key: {@code true} only
     * when a route from {@code from} to {@code to} exists <em>and</em> both dimensions are loaded.
     */
    public static boolean isOpen(MinecraftServer server, ResourceKey<Level> from, ResourceKey<Level> to) {
        if (server == null) {
            return false;
        }
        return route(from, to).map(r -> isOpen(server, r)).orElse(false);
    }

    /**
     * The lowest tier (1-based) whose destination unlocks include {@code dimension}. The Overworld needs
     * no unlock (every rocket can fly home), so it contributes tier 1.
     */
    private static int minimumTier(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) {
            return 1;
        }
        for (RocketTier tier : RocketTier.values()) {
            if (tier.destinations().contains(dimension)) {
                return tier.level();
            }
        }
        return MAX_ROCKET_TIER;
    }
}
