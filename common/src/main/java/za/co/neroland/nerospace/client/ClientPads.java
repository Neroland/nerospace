package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.network.PadSyncPayload;

/**
 * Client-side holder for the selectable landing nodes' labels per dimension, fed by {@link PadSyncPayload} when
 * the player opens a rocket. Pure data — safe to reference from the common handler registration.
 */
public final class ClientPads {

    private static final Map<String, List<String>> NAMES = new HashMap<>();

    private ClientPads() {
    }

    public static void accept(PadSyncPayload payload) {
        NAMES.clear();
        for (int i = 0; i < payload.dims().size(); i++) {
            NAMES.computeIfAbsent(payload.dims().get(i), k -> new ArrayList<>()).add(payload.names().get(i));
        }
    }

    /** The label of pad {@code index} in {@code dimId}, or {@code null} when unknown. */
    @Nullable
    public static String name(String dimId, int index) {
        List<String> list = NAMES.get(dimId);
        return list != null && index >= 0 && index < list.size() ? list.get(index) : null;
    }

    /** How many selectable pads the client knows in {@code dimId}. */
    public static int count(String dimId) {
        List<String> list = NAMES.get(dimId);
        return list == null ? 0 : list.size();
    }
}
