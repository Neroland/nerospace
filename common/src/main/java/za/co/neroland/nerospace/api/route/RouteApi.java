package za.co.neroland.nerospace.api.route;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/**
 * The cargo-route entry point for other mods. <b>Public API — semver-stable.</b> Obtain it with
 * {@link #instance()}; every method is server-side and must be called on the server thread. Nothing here
 * loads a dimension or a chunk, and nothing here exposes a player UUID other than the ones the caller
 * passes in.
 *
 * <p>Typical NeroLogistics use, replacing its stub {@code RouteProvider}:</p>
 * <pre>{@code
 * RouteApi api = RouteApi.instance();
 * List<PadHandle> destinations = api.padsVisibleTo(server, owner);        // "destinations for this origin"
 * RouteQuote quote = api.quote(server, origin, dest, manifest, false).orElseThrow();
 * FlightRequestResult r = api.requestFlight(server, owner, new FlightRequest(origin, dest, manifest, false));
 * RouteEvents.subscribe(new RouteEvents.Listener() { ... onFlightArrived ... });
 * }</pre>
 */
public interface RouteApi {

    /** The live implementation. Never {@code null}; safe to call from mod construction onwards. */
    static RouteApi instance() {
        return za.co.neroland.nerospace.route.RouteApiImpl.INSTANCE;
    }

    /**
     * The pads {@code player} may ship to: pads they own, pads whose access list names them, public pads,
     * and stations they may manage. Never a server-wide roster. Empty for a {@code null} player.
     */
    List<PadHandle> padsVisibleTo(MinecraftServer server, UUID player);

    /** One pad or station by id, regardless of visibility (ids are not secret; positions of others' pads are not returned elsewhere). */
    Optional<PadHandle> pad(MinecraftServer server, int padId);

    /**
     * Price a shipment. Empty when either id is unknown, both are the same pad, or the origin is a
     * station (stations cannot launch cargo). Does not check permissions or fuel — see
     * {@link #requestFlight} for the authoritative gate.
     */
    Optional<RouteQuote> quote(MinecraftServer server, int originPadId, int destinationPadId,
            List<ItemStack> manifest, boolean returnEmpty);

    /**
     * Launch {@code request.manifest()} now from the docked, fuelled cargo rocket on the origin pad. On
     * success the manifest is aboard the returned flight and the origin pad's <em>own</em> inventory is
     * untouched — the caller supplies the items. See {@link FlightRequest} and
     * {@link FlightRequestResult.Denial} for every refusal reason.
     */
    FlightRequestResult requestFlight(MinecraftServer server, UUID requester, FlightRequest request);

    /** A flight by id (live or retained), or empty once pruned. */
    Optional<FlightHandle> flight(MinecraftServer server, int flightId);

    /** Live and retained flights owned by {@code player}, oldest first. Empty for {@code null}. */
    List<FlightHandle> flightsOwnedBy(MinecraftServer server, UUID player);

    /** Saved routes owned by {@code player}. Empty for {@code null}. */
    List<RouteHandle> routesOwnedBy(MinecraftServer server, UUID player);

    /**
     * Cancel a flight the requester owns while it is {@link FlightState#HOLDING} or
     * {@link FlightState#AWAITING_CHUNK}: the remaining manifest is crated at the destination as soon as
     * that position is loaded. Returns {@code false} when the flight is unknown, not the requester's, or in
     * any other state — an in-flight rocket cannot be recalled and a delivered one is done.
     */
    boolean cancel(MinecraftServer server, UUID requester, int flightId);
}
