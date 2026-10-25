package za.co.neroland.nerospace.world.station;

/**
 * The numbers one {@link StationClass} grants. Pure data; the console, the public API and the
 * gameplay hooks read these and nothing else, so a class is exactly one headline number plus its module gate.
 *
 * @param routeCapacity        logistics routes the station can anchor ({@code StationInfo.routeCapacity()})
 * @param starGuideSpeed       timed progression tempo for {@code canUse} players aboard (RESEARCH 1.25)
 * @param solarWingMultiplier  Solar Wing output multiplier (INDUSTRIAL 1.5)
 * @param hydroponicsMultiplier crop growth chance on trays aboard (INDUSTRIAL 0.5)
 * @param observatoryAllowed   whether the Observatory module may be built (RESEARCH only)
 */
public record StationTraits(int routeCapacity, double starGuideSpeed, double solarWingMultiplier,
        double hydroponicsMultiplier, boolean observatoryAllowed) {
}
