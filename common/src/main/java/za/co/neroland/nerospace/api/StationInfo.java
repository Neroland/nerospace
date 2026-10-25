package za.co.neroland.nerospace.api;

import net.minecraft.core.BlockPos;

/**
 * Read-only, immutable description of a founded station destination.
 *
 * <p><b>Public API — semver-stable.</b> A privacy-safe projection of the internal
 * {@code rocket.StationRegistry.StationEntry}: it deliberately omits the founder's UUID. Ownership is
 * exposed only as a boolean check ({@link NerospaceStations#ownedBy}). Obtain via {@link NerospaceStations}.</p>
 *
 * @param id            the stable station id (its slot number; never reused, so a stable routing key)
 * @param name          the display name
 * @param planet        the planet/dimension the station lives in (always {@link NerospacePlanets#STATION})
 * @param position      the station centre in that dimension (immutable)
 * @param routeCapacity the number of logistics routes this station can anchor — for consumers like
 *                      NeroLogistics. Varies by class since 2.0.0 (WAYSTATION 9, TRADE 10, otherwise 8); see
 *                      {@link NerospaceStations#DEFAULT_ROUTE_CAPACITY}.
 * @param stationClass  the station's class by name — {@code "WAYSTATION"}, {@code "RESEARCH"}, {@code "INDUSTRIAL"}
 *                      or {@code "TRADE"} (since 2.0.0). Consumers derive traits by name: a beam origin is
 *                      {@code "INDUSTRIAL".equals(stationClass())}, a market node {@code "TRADE".equals(...)}.
 *                      An unknown future name should be treated as {@code "WAYSTATION"}.
 */
public record StationInfo(int id, String name, PlanetId planet, BlockPos position, int routeCapacity,
        String stationClass) {

    /** The class a station reports when none is known (the 1.x five-argument shape). */
    public static final String DEFAULT_CLASS = "WAYSTATION";

    /** The 1.6.0 five-argument shape, kept for source and binary compatibility; the class is {@code WAYSTATION}. */
    public StationInfo(int id, String name, PlanetId planet, BlockPos position, int routeCapacity) {
        this(id, name, planet, position, routeCapacity, DEFAULT_CLASS);
    }

    public StationInfo {
        if (stationClass == null || stationClass.isBlank()) {
            stationClass = DEFAULT_CLASS;
        }
        if (name == null) {
            throw new IllegalArgumentException("StationInfo name must not be null");
        }
        if (planet == null) {
            throw new IllegalArgumentException("StationInfo planet must not be null");
        }
        if (position == null) {
            throw new IllegalArgumentException("StationInfo position must not be null");
        }
    }
}
