package za.co.neroland.nerospace.api.route;

import java.util.List;
import java.util.UUID;

import net.minecraft.world.item.ItemStack;

/**
 * An immutable snapshot of one cargo flight: one rocket, one manifest. <b>Public API — semver-stable.</b>
 *
 * <p>Ticks are absolute overworld game time (they persist across restarts, so a timer resumes exactly
 * where it stopped). The manifest is a defensive copy of what is still aboard: it shrinks while the
 * flight is {@link FlightState#UNLOADING} and is empty once {@link FlightState#DELIVERED}.</p>
 */
public interface FlightHandle {

    /** Stable id, never reused. Safe to log and to put in a telemetry breadcrumb. */
    int id();

    /** The saved route this flight follows, or {@code -1} for an ad-hoc API flight. */
    int routeId();

    int originPadId();

    int destinationPadId();

    FlightState state();

    /** Overworld game time at launch. */
    long departedAt();

    /** Overworld game time the timer elapses (arrival may still be deferred or held after this). */
    long arrivesAt();

    /** Whether this is the empty return leg of a {@code return_empty} route. */
    boolean returnLeg();

    /** Copies of the stacks still aboard. Never the live list. */
    List<ItemStack> manifest();

    /** Total item count still aboard (a cheap size check for dashboards). */
    int itemCount();

    /** Whether {@code player} owns this flight (the route owner at launch; {@code false} once erased). */
    boolean ownedBy(UUID player);
}
