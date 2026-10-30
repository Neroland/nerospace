package za.co.neroland.nerospace.world.station;

import net.minecraft.server.level.ServerLevel;

import za.co.neroland.nerospace.rocket.StationRegistry.StationEntry;

/**
 * Seams the rocket package fills in, so the station code needs no compile dependency on rocket internals.
 */
public final class StationHooks {

    /** {@link VisitorSender#sendHome} result: refused, a player who cannot use the station is still aboard. */
    public static final int VISITOR_ABOARD = -2;

    /**
     * "Send riderless visitor rocket home": flies every <b>riderless</b> visitor rocket that has been parked
     * at least ten minutes on a dock of this station, blocking it, back to its own return pad (or a Landing Pod by the
     * normal resolver in its origin dimension) and tells the players on the station. A rocket with a rider is never
     * moved, and nothing is sent while a player who cannot use the station is still aboard.
     */
    @FunctionalInterface
    public interface VisitorSender {
        /**
         * @return how many rockets were sent home, {@code -1} when the feature is unavailable, or
         *         {@link #VISITOR_ABOARD}.
         */
        int sendHome(ServerLevel stationLevel, StationEntry entry);
    }

    private static volatile VisitorSender visitorSender = (level, entry) -> -1;

    private StationHooks() {
    }

    /** Installed once by the rocket package's init. */
    public static void setVisitorSender(VisitorSender sender) {
        if (sender != null) {
            visitorSender = sender;
        }
    }

    public static int sendVisitorsHome(ServerLevel stationLevel, StationEntry entry) {
        return visitorSender.sendHome(stationLevel, entry);
    }
}
