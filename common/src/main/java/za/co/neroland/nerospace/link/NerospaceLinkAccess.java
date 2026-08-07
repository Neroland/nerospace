package za.co.neroland.nerospace.link;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.rocket.StationRegistry;

/**
 * The one place Nerospace's link-module visibility rule lives, plus the plumbing every surface needs: the
 * running server handle, the online lookup, and null-safe JSON parameter readers.
 *
 * <p>Core's SPI hands a provider nothing but a {@link UUID}, so the module needs its own server handle.
 * Each loader's server-tick hook calls {@link #rememberServer(MinecraftServer)} beside the existing
 * {@code MeteorEvents.tick} call, mirroring how NeroTech captures its server. Before the first world load
 * (and on a client that has left its integrated server) {@link #server()} answers {@code null}, and every
 * caller must then answer "nothing" rather than guess.</p>
 *
 * <p><b>POPIA/GDPR.</b> {@link #stationsOf(MinecraftServer, UUID)} is the ownership rule for the whole
 * module and it never widens — not for an operator, not for a station with a blank owner. A station that
 * is not the requester's reads exactly like a station that does not exist, so the bridge cannot be used to
 * probe for other players' slots.</p>
 */
final class NerospaceLinkAccess {

    /**
     * How far, in blocks, the {@code rockets} section looks around an online player for rockets and
     * launch pads. This is the module's substitute for ownership on unowned world content: it reports
     * what the requester could already see by standing where they are, not a server-wide roster that
     * would map every base and pad on the server.
     */
    static final int SCAN_RADIUS = 128;

    /**
     * The running server, captured from each loader's server-tick hook. Volatile — written from the server
     * thread, read from whichever thread Core's bridge dispatches on. Re-written every tick, so it
     * self-corrects when a new world is loaded.
     */
    @Nullable
    private static volatile MinecraftServer server;

    private NerospaceLinkAccess() {
    }

    /** Captures the running server. Called once per server tick from every loader entry point. */
    static void rememberServer(MinecraftServer runningServer) {
        server = runningServer;
    }

    /** The running server, or {@code null} before the first world load. */
    @Nullable
    static MinecraftServer server() {
        return server;
    }

    /** Whether the module is switched on right now (re-read on every call, so a config reload takes effect). */
    static boolean enabled() {
        try {
            return NerospaceConfig.linkModuleEnabled();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** The requester's live player handle, or {@code null} when they are offline. */
    @Nullable
    static ServerPlayer online(@Nullable MinecraftServer runningServer, @Nullable UUID playerId) {
        if (runningServer == null || playerId == null) {
            return null;
        }
        return runningServer.getPlayerList().getPlayer(playerId);
    }

    /** Whether the requester is online. Reported in every snapshot envelope so a client can say why a section is empty. */
    static boolean isOnline(@Nullable MinecraftServer runningServer, @Nullable UUID playerId) {
        return online(runningServer, playerId) != null;
    }

    /**
     * THE visibility rule: the stations whose stored owner UUID is exactly the requester's.
     *
     * <p>Legacy/unowned entries (owner {@code ""} — including entries anonymised by an erasure request)
     * belong to nobody and are therefore returned to nobody. Operator status is deliberately not honoured:
     * an operator's powers are a property of a live command source, not of a UUID arriving over a bridge,
     * and honouring them here would turn "I am an admin" into "my phone can read every station on the
     * server".</p>
     */
    static List<StationRegistry.StationEntry> stationsOf(@Nullable MinecraftServer runningServer,
            @Nullable UUID playerId) {
        if (runningServer == null || playerId == null) {
            return List.of();
        }
        String owner = playerId.toString();
        List<StationRegistry.StationEntry> out = new ArrayList<>();
        for (StationRegistry.StationEntry entry : StationRegistry.get(runningServer).all()) {
            if (owner.equals(entry.owner())) {
                out.add(entry);
            }
        }
        return out;
    }

    /**
     * Narrows the requester's own stations by an optional {@code station=<slot>} query parameter. Fails
     * CLOSED: an unparseable or unowned slot yields an empty list, never the full set.
     */
    static List<StationRegistry.StationEntry> requestedStations(@Nullable MinecraftServer runningServer,
            @Nullable UUID playerId, @Nullable Map<String, String> params) {
        List<StationRegistry.StationEntry> visible = stationsOf(runningServer, playerId);
        String wanted = params == null ? null : params.get("station");
        if (wanted == null || wanted.isBlank()) {
            return visible;
        }
        Integer slot = parseSlot(wanted);
        if (slot == null) {
            return List.of();
        }
        for (StationRegistry.StationEntry entry : visible) {
            if (entry.slot() == slot) {
                return List.of(entry);
            }
        }
        return List.of();
    }

    /**
     * The station named by an action's {@code station} parameter, or {@code null}. Resolved by scanning
     * only {@link #stationsOf}, so "not yours" and "does not exist" are the same answer.
     */
    @Nullable
    static StationRegistry.StationEntry stationParam(@Nullable MinecraftServer runningServer,
            @Nullable UUID playerId, @Nullable JsonObject params) {
        Integer slot = integer(params, "station");
        if (slot == null) {
            return null;
        }
        for (StationRegistry.StationEntry entry : stationsOf(runningServer, playerId)) {
            if (entry.slot() == slot) {
                return entry;
            }
        }
        return null;
    }

    /** A string parameter, or {@code null} when absent / not a primitive. Never throws. */
    @Nullable
    static String string(@Nullable JsonObject params, String key) {
        if (params == null || !params.has(key)) {
            return null;
        }
        JsonElement element = params.get(key);
        if (!element.isJsonPrimitive()) {
            return null;
        }
        String value = element.getAsString();
        return value == null || value.isEmpty() ? null : value;
    }

    /** An integer parameter, or {@code null} when absent / not a number. Never throws. */
    @Nullable
    static Integer integer(@Nullable JsonObject params, String key) {
        if (params == null || !params.has(key)) {
            return null;
        }
        JsonElement element = params.get(key);
        if (!element.isJsonPrimitive()) {
            return null;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private static Integer parseSlot(String raw) {
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
