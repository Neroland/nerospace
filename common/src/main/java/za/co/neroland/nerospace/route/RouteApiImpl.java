package za.co.neroland.nerospace.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.api.route.FlightHandle;
import za.co.neroland.nerospace.api.route.FlightRequest;
import za.co.neroland.nerospace.api.route.FlightRequestResult;
import za.co.neroland.nerospace.api.route.FlightState;
import za.co.neroland.nerospace.api.route.PadHandle;
import za.co.neroland.nerospace.api.route.RouteApi;
import za.co.neroland.nerospace.api.route.RouteHandle;
import za.co.neroland.nerospace.api.route.RouteQuote;

/**
 * The {@link RouteApi} implementation: a thin adapter over {@link RouteRegistry} and {@link CargoLaunch}.
 * Internal — consumers reach it only through {@link RouteApi#instance()}; nothing here is semver-bound.
 */
public final class RouteApiImpl implements RouteApi {

    public static final RouteApiImpl INSTANCE = new RouteApiImpl();

    private RouteApiImpl() {
    }

    @Override
    public List<PadHandle> padsVisibleTo(MinecraftServer server, UUID player) {
        if (server == null || player == null) {
            return List.of();
        }
        return RouteRegistry.get(server).padsVisibleTo(server, player);
    }

    @Override
    public Optional<PadHandle> pad(MinecraftServer server, int padId) {
        if (server == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(RouteRegistry.get(server).resolve(server, padId));
    }

    @Override
    public Optional<RouteQuote> quote(MinecraftServer server, int originPadId, int destinationPadId,
            List<ItemStack> manifest, boolean returnEmpty) {
        if (server == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(CargoLaunch.quote(server, RouteRegistry.get(server), originPadId, destinationPadId,
                manifest == null ? List.of() : manifest, returnEmpty));
    }

    @Override
    public FlightRequestResult requestFlight(MinecraftServer server, UUID requester, FlightRequest request) {
        if (server == null || request == null) {
            return FlightRequestResult.denied(FlightRequestResult.Denial.NO_ORIGIN);
        }
        return CargoLaunch.launch(server, RouteRegistry.get(server), requester, -1, request.originPadId(),
                request.destinationPadId(), request.manifest(), request.returnEmpty(), null);
    }

    @Override
    public Optional<FlightHandle> flight(MinecraftServer server, int flightId) {
        if (server == null) {
            return Optional.empty();
        }
        FlightRecord flight = RouteRegistry.get(server).flight(flightId);
        return flight == null ? Optional.empty() : Optional.of(flight.view());
    }

    @Override
    public List<FlightHandle> flightsOwnedBy(MinecraftServer server, UUID player) {
        if (server == null || player == null) {
            return List.of();
        }
        List<FlightHandle> out = new ArrayList<>();
        for (FlightRecord flight : RouteRegistry.get(server).flightsOwnedBy(player)) {
            out.add(flight.view());
        }
        return List.copyOf(out);
    }

    @Override
    public List<RouteHandle> routesOwnedBy(MinecraftServer server, UUID player) {
        if (server == null || player == null) {
            return List.of();
        }
        List<RouteHandle> out = new ArrayList<>();
        for (RouteRecord route : RouteRegistry.get(server).routesOwnedBy(player)) {
            out.add(route.view());
        }
        return List.copyOf(out);
    }

    @Override
    public boolean cancel(MinecraftServer server, UUID requester, int flightId) {
        if (server == null || requester == null) {
            return false;
        }
        RouteRegistry registry = RouteRegistry.get(server);
        FlightRecord flight = registry.flight(flightId);
        if (flight == null || !flight.ownedBy(requester)) {
            return false;
        }
        if (flight.state() != FlightState.HOLDING && flight.state() != FlightState.AWAITING_CHUNK) {
            return false;
        }
        flight.requestCancel();
        registry.flightChanged();
        return true;
    }
}
