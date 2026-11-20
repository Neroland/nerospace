package za.co.neroland.nerospace.world.planet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

/**
 * Pure rules for a planet with two weather events (a primary and an optional secondary). Each startable event rolls
 * independently with its own parameters, the secondary first; an active event is owned by the parameters whose event
 * it is. A planet without a secondary behaves exactly as a single-event planet.
 */
public final class WeatherPickRules {

    private WeatherPickRules() {
    }

    /** Order in which start rolls are made this step: secondary first, then primary. Empty entries are skipped. */
    public static List<WeatherParams> startOrder(WeatherParams primary, WeatherParams secondary) {
        List<WeatherParams> out = new ArrayList<>(2);
        if (secondary != null && secondary.hasWeather()) {
            out.add(secondary);
        }
        if (primary != null && primary.hasWeather()) {
            out.add(primary);
        }
        return List.copyOf(out);
    }

    /** Params that own an active event (duration, cooldown): the secondary for its event, else the primary. */
    public static WeatherParams owner(WeatherParams primary, WeatherParams secondary, WeatherEvent active) {
        if (secondary != null && secondary.hasWeather() && active != null && active != WeatherEvent.NONE
                && secondary.event() == active) {
            return secondary;
        }
        return primary;
    }

    /**
     * Whether the secondary gets its own start roll this step: only on a planet that has one, and only while the
     * machine is clear with its cooldown run out. In every other case the step is the single primary call.
     */
    public static boolean rollsSecondary(WeatherParams secondary, boolean machineActive, int cooldownTicks) {
        return secondary != null && secondary.hasWeather() && !machineActive && cooldownTicks == 0;
    }

    /**
     * One weather step for a planet. With no secondary this is exactly one {@code machine.step(primary, ...)}, the
     * same call (and the same draws from {@code random}) as a single-event planet has always made. With a secondary:
     * an active event is stepped with the params that own it; a clear machine whose cooldown has run out rolls the
     * secondary first and, unless that started, the primary; in every other case the primary is stepped alone. The
     * machine draws from {@code random} only when the event may start.
     *
     * @param primary   the planet's primary params, already config-scaled
     * @param secondary the secondary params, already config-scaled, or {@link WeatherParams#NONE}
     * @param mayStart  whether an event may begin right now, per event
     */
    public static WeatherMachine.Change step(WeatherMachine machine, WeatherParams primary, WeatherParams secondary,
            boolean enabled, Predicate<WeatherEvent> mayStart, DoubleSupplier random) {
        if (secondary == null || !secondary.hasWeather()) {
            return machine.step(primary, enabled, mayStart.test(primary.event()), random);
        }
        if (machine.active()) {
            WeatherParams owner = owner(primary, secondary, machine.event);
            return machine.step(owner, enabled, mayStart.test(owner.event()), random);
        }
        if (rollsSecondary(secondary, false, machine.cooldownTicks)) {
            WeatherMachine.Change change = machine.step(secondary, enabled, mayStart.test(secondary.event()), random);
            if (change == WeatherMachine.Change.STARTED) {
                return change;
            }
        }
        return machine.step(primary, enabled, mayStart.test(primary.event()), random);
    }

    /** {@code 2.0 * eligibleTicksPerDay / meanIntervalTicks}: the upper bound on starts per day. */
    public static double expectedStartsPerDay(WeatherParams p, double eligibleFraction, long dayLengthTicks) {
        if (p == null || !p.hasWeather()) {
            return 0.0;
        }
        return 2.0 * eligibleFraction * dayLengthTicks / p.meanIntervalTicks();
    }
}
