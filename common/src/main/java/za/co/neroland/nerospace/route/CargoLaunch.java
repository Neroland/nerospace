package za.co.neroland.nerospace.route;

import java.util.List;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.api.route.FlightRequestResult;
import za.co.neroland.nerospace.api.route.FlightRequestResult.Denial;
import za.co.neroland.nerospace.api.route.PadHandle;
import za.co.neroland.nerospace.api.route.RouteQuote;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;

/**
 * The one launch gate. Both the pad (button or schedule) and the API come through {@link #launch}, so the
 * checks — permission, docked rocket, pad tier, manifest cap, fuel, per-owner cap — are identical, and a
 * launch that passes is recorded in the {@link RouteRegistry} <em>before</em> the rocket lifts off. Server
 * thread only.
 *
 * <p><b>Privacy:</b> the breadcrumb carries the flight id only.</p>
 */
public final class CargoLaunch {

    /** The pad formation a cargo rocket needs under it: a complete 3×3 (tier 2), same as a Tier-2 crewed rocket. */
    public static final int REQUIRED_PAD_TIER = 2;

    private CargoLaunch() {
    }

    /** Quote a pair of endpoints for {@code manifest}; empty when either endpoint is unknown or the origin is a station. */
    public static RouteQuote quote(MinecraftServer server, RouteRegistry registry, int originPadId, int destinationPadId,
            List<ItemStack> manifest, boolean returnEmpty) {
        RouteRegistry.Endpoint origin = registry.endpoint(server, originPadId);
        RouteRegistry.Endpoint destination = registry.endpoint(server, destinationPadId);
        if (origin == null || destination == null || origin.station() || originPadId == destinationPadId) {
            return null;
        }
        double gO = CargoFormulas.gravityOf(origin.dimension());
        double gD = CargoFormulas.gravityOf(destination.dimension());
        return CargoFormulas.quote(gO, gD, origin.dimension().equals(destination.dimension()), manifest, returnEmpty);
    }

    /** The pad's own Launch: carries whatever is in the hold along the pad's configured route. */
    public static FlightRequestResult launchFromPad(ServerLevel level, CargoPadBlockEntity pad, UUID requester) {
        RouteRegistry registry = RouteRegistry.get(level.getServer());
        PadRecord record = pad.record();
        if (record == null) {
            return FlightRequestResult.denied(Denial.NO_ORIGIN);
        }
        RouteRecord route = registry.routeForOrigin(record.id());
        if (route == null) {
            return FlightRequestResult.denied(Denial.NO_DESTINATION);
        }
        List<ItemStack> manifest = pad.manifestCopy();
        FlightRequestResult result = launch(level.getServer(), registry, requester, route.id(), record.id(),
                route.destinationPadId(), manifest, route.returnEmpty(), pad);
        if (result.accepted()) {
            pad.takeManifest(); // the copies aboard the flight are now the only copies
        }
        return result;
    }

    /**
     * The gate. {@code pad} may be passed when the caller already holds the origin block entity; otherwise it
     * is looked up (without loading anything).
     */
    public static FlightRequestResult launch(MinecraftServer server, RouteRegistry registry, @Nullable UUID requester,
            int routeId, int originPadId, int destinationPadId, List<ItemStack> manifest, boolean returnEmpty,
            @Nullable CargoPadBlockEntity pad) {
        PadRecord origin = registry.pad(originPadId);
        if (origin == null) {
            return FlightRequestResult.denied(Denial.NO_ORIGIN);
        }
        PadHandle destination = registry.resolve(server, destinationPadId);
        if (destination == null || destinationPadId == originPadId) {
            return FlightRequestResult.denied(Denial.NO_DESTINATION);
        }
        if (requester == null || !origin.accessibleBy(requester) || !destination.accessibleBy(requester)) {
            return FlightRequestResult.denied(Denial.NOT_PERMITTED);
        }
        if (registry.liveFlightsOwnedBy(requester) >= NerospaceConfig.cargoMaxFlightsPerOwner()) {
            return FlightRequestResult.denied(Denial.TOO_MANY_FLIGHTS);
        }

        ServerLevel level = server.getLevel(origin.dimension());
        if (level == null || !level.hasChunk(origin.pos().getX() >> 4, origin.pos().getZ() >> 4)) {
            return FlightRequestResult.denied(Denial.ORIGIN_NOT_LOADED);
        }
        CargoPadBlockEntity originPad = pad != null ? pad
                : level.getBlockEntity(origin.pos()) instanceof CargoPadBlockEntity be ? be : null;
        if (originPad == null) {
            return FlightRequestResult.denied(Denial.ORIGIN_NOT_LOADED);
        }
        CargoRocketEntity rocket = originPad.dockedRocket();
        if (rocket == null || rocket.isLaunching()) {
            return FlightRequestResult.denied(Denial.NO_ROCKET);
        }
        if (originPad.padTier() < REQUIRED_PAD_TIER) {
            return FlightRequestResult.denied(Denial.PAD_TOO_SMALL);
        }

        List<ItemStack> items = manifest.stream().filter(s -> s != null && !s.isEmpty()).map(ItemStack::copy).toList();
        if (items.isEmpty() || items.size() > originPad.activeSlots()) {
            return FlightRequestResult.denied(Denial.BAD_MANIFEST);
        }

        RouteQuote quote = quote(server, registry, originPadId, destinationPadId, items, returnEmpty);
        if (quote == null) {
            return FlightRequestResult.denied(Denial.NO_DESTINATION);
        }
        if (rocket.getFuel() < quote.fuelMb()) {
            return FlightRequestResult.denied(Denial.NOT_ENOUGH_FUEL);
        }

        // --- Commit: fuel, record, lift-off — in that order, so a crash between steps loses fuel, never cargo.
        int burned = rocket.burnFuel(quote.fuelMb());
        int carried = Math.max(0, rocket.getFuel());
        RouteRegistry.Endpoint from = registry.endpoint(server, originPadId);
        RouteRegistry.Endpoint to = registry.endpoint(server, destinationPadId);
        long now = server.overworld().getGameTime();
        FlightRecord flight = registry.createFlight(routeId, originPadId, destinationPadId, requester.toString(),
                false, from, to, carried, items, now, quote.travelTicks());
        rocket.liftOff();
        NerospaceTelemetry.breadcrumb("cargo", "depart flight=" + flight.id() + " ticks=" + quote.travelTicks()
                + " fuel=" + burned);
        RouteEventDispatch.departed(flight.view());
        return FlightRequestResult.accepted(flight.view());
    }
}
