package za.co.neroland.nerospace.api.route;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * A request to fly {@code manifest} from one pad to another on behalf of a player. <b>Public API —
 * semver-stable.</b>
 *
 * <p>The requester must be able to access the origin pad (owner or access list) and the destination
 * ({@link PadHandle#accessibleBy}). Fuel is drawn from the rocket docked on the origin pad — a docked,
 * fuelled cargo rocket is required exactly as for a manual launch, so the API never conjures a rocket.
 * The manifest is copied; the caller is responsible for having removed those items from wherever they
 * came from <em>after</em> the request succeeds (check {@link FlightRequestResult#accepted()}).</p>
 *
 * @param originPadId      the pad the rocket departs from (must be a Cargo Pad, not a station)
 * @param destinationPadId the pad or station to deliver to
 * @param manifest         the stacks to carry; empty stacks are ignored, the total must fit the
 *                         server's {@code cargoPadSlots} cap
 * @param returnEmpty      fly the rocket back empty after unloading (fuel for the return leg is charged
 *                         at launch)
 */
public record FlightRequest(int originPadId, int destinationPadId, List<ItemStack> manifest, boolean returnEmpty) {

    public FlightRequest {
        if (manifest == null) {
            throw new IllegalArgumentException("FlightRequest manifest must not be null");
        }
        manifest = List.copyOf(manifest);
    }
}
