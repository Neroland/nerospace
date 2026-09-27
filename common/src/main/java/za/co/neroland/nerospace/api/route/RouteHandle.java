package za.co.neroland.nerospace.api.route;

import java.util.UUID;

/**
 * An immutable snapshot of a saved route: {@code origin pad → destination pad} plus its schedule.
 * <b>Public API — semver-stable.</b> Routes are created from the Cargo Pad GUI; the API reads them and
 * launches flights along them (or ad-hoc, see {@link RouteApi#requestFlight}).
 */
public interface RouteHandle {

    /** Stable id, never reused. */
    int id();

    /** The pad the rocket departs from. */
    int originPadId();

    /** The pad (or station) the rocket lands on. */
    int destinationPadId();

    /** How the origin pad schedules launches on this route. */
    ScheduleMode scheduleMode();

    /** Interval for {@link ScheduleMode#EVERY_INTERVAL}, in ticks. */
    int intervalTicks();

    /** Whether the rocket flies back empty after unloading instead of staying on the destination pad. */
    boolean returnEmpty();

    /** Whether {@code player} owns this route (the origin pad's owner at creation). */
    boolean ownedBy(UUID player);
}
