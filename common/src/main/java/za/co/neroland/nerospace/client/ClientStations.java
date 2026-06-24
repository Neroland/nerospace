package za.co.neroland.nerospace.client;

import java.util.HashMap;
import java.util.Map;

import za.co.neroland.nerospace.network.StationSyncPayload;

/**
 * Client-side holder for the founded stations' display names (slot → name), fed by
 * {@link StationSyncPayload} (the clientbound handler registered in {@code ModNetwork.init()}) when the
 * player opens a rocket, and read by {@code RocketScreen} to label the "Dock:" cycler. Pure data — no
 * client-only imports — so it loads safely even where the handler is registered from common code.
 */
public final class ClientStations {

    private static final Map<Integer, String> NAMES = new HashMap<>();

    private ClientStations() {
    }

    public static void accept(StationSyncPayload payload) {
        NAMES.clear();
        int[] slots = payload.slots();
        String[] names = payload.names();
        for (int i = 0; i < slots.length; i++) {
            NAMES.put(slots[i], names[i]);
        }
    }

    /**
     * The selected docking target's label: the shared origin platform for {@code slot < 0}, the synced
     * charter name when known, else the stable founding-order fallback ("Station N").
     */
    public static String name(int slot) {
        if (slot < 0) {
            return "Origin Platform";
        }
        return NAMES.getOrDefault(slot, "Station " + (slot + 1));
    }
}
