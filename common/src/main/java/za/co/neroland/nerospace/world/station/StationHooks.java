package za.co.neroland.nerospace.world.station;

import net.minecraft.server.level.ServerLevel;

import za.co.neroland.nerospace.rocket.StationRegistry.StationEntry;

/**
 * Seams the rocket package fills in, so the station code needs no compile dependency on rocket internals.
 */
public final class StationHooks {

    /**
     * "Send riderless visitor rocket home": flies every <b>riderless</b> rocket that has been parked
     * at least ten minutes on a dock of this station, blocking it, back to its own return pad (or a Landing Pod by the
     * normal resolver in its origin dimension) and messages its owner. A rocket with a rider is never moved.
     */
    @FunctionalInterface
    public interface VisitorSender {
        /** @return how many rockets were sent home, or {@code -1} when the feature is unavailable. */
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
