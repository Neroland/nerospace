package za.co.neroland.nerospace.client;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.network.StationConsoleSyncPayload;

/**
 * Client-side holder for the open Station Console's last {@link StationConsoleSyncPayload}. A pure data holder (no
 * client-only imports), so registering {@link #accept} from common code is safe on a dedicated server. Nothing is
 * persisted; the value is dropped when the console closes.
 */
public final class ClientStationConsole {

    @Nullable
    private static volatile StationConsoleSyncPayload current;
    private static volatile long receivedAt;

    private ClientStationConsole() {
    }

    public static void accept(StationConsoleSyncPayload payload) {
        current = payload;
        receivedAt = System.currentTimeMillis();
    }

    /** The payload for {@code containerId}, or {@code null} while syncing. */
    @Nullable
    public static StationConsoleSyncPayload forContainer(int containerId) {
        StationConsoleSyncPayload p = current;
        return p != null && p.containerId() == containerId ? p : null;
    }

    public static long receivedAt() {
        return receivedAt;
    }

    public static void clear() {
        current = null;
    }
}
