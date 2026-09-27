package za.co.neroland.nerospace.client;

import za.co.neroland.nerospace.network.CargoPadSyncPayload;

/**
 * Client-side holder for the destination list of the currently open Cargo Pad GUI, fed by
 * {@link CargoPadSyncPayload}. Pure data — no client-only imports — so the handler is safe to register from
 * common code. Only the latest payload is kept; a menu id mismatch simply reads as "no destinations".
 */
public final class ClientCargoPads {

    private static volatile CargoPadSyncPayload latest;

    private ClientCargoPads() {
    }

    public static void accept(CargoPadSyncPayload payload) {
        latest = payload;
    }

    public static int count(int containerId) {
        CargoPadSyncPayload p = latest;
        return p == null || p.containerId() != containerId ? 0 : p.ids().length;
    }

    /** The label for destination {@code index}, or an em dash when nothing is selected / known. */
    public static String label(int containerId, int index) {
        CargoPadSyncPayload p = latest;
        if (p == null || p.containerId() != containerId || index < 0 || index >= p.ids().length) {
            return "—";
        }
        String where = p.dimensions()[index];
        return p.names()[index] + " (" + where + ")";
    }
}
