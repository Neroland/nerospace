package za.co.neroland.nerospace.api.route;

/**
 * How a Cargo Pad decides to launch on its selected route. <b>Public API — semver-stable.</b> Scheduling
 * runs in the pad's own tick (the pad must be loaded to hold a rocket anyway); NeroLogistics layers its
 * network-aware scheduling on top through {@link RouteApi#requestFlight} rather than replacing this.
 */
public enum ScheduleMode {

    /** Only the Launch button (or an API request) launches. */
    MANUAL,
    /** Launch as soon as every usable cargo slot holds something and the rocket is fuelled. */
    WHEN_FULL,
    /** Launch every {@code intervalTicks} when there is at least one item aboard and fuel for the trip. */
    EVERY_INTERVAL;

    /** Safe lookup by ordinal for persistence / synced data. */
    public static ScheduleMode byOrdinal(int ordinal) {
        ScheduleMode[] values = values();
        return ordinal < 0 || ordinal >= values.length ? MANUAL : values[ordinal];
    }

    /** The next mode in the cycle the pad GUI uses. */
    public ScheduleMode next() {
        return byOrdinal((ordinal() + 1) % values().length);
    }
}
