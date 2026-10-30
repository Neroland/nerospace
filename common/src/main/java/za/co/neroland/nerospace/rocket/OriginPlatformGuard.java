package za.co.neroland.nerospace.rocket;

/**
 * The origin-platform re-stamp fix: the shared origin platform has no registry entry, so a
 * "Station Core at centre" guard never trips. It counts as built when its pad node is registered or its
 * centre floor is a station floor; then the arrival only re-registers the node and writes no block.
 */
public final class OriginPlatformGuard {

    private OriginPlatformGuard() {
    }

    public static boolean alreadyBuilt(boolean hasCoreAtCentre, boolean hasPadNodeAtPad, boolean floorIsStationFloor) {
        return hasCoreAtCentre || hasPadNodeAtPad || floorIsStationFloor;
    }
}
