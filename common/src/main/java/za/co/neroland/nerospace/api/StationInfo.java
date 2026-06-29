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
 *                      NeroLogistics. See {@link NerospaceStations#DEFAULT_ROUTE_CAPACITY}.
 */
public record StationInfo(int id, String name, PlanetId planet, BlockPos position, int routeCapacity) {

    public StationInfo {
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
