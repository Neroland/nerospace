package za.co.neroland.nerospace.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.rocket.StationRegistry.StationEntry;

/**
 * Public, read-only facade over the founded-station / destination registry.
 *
 * <p><b>Public API — semver-stable.</b> Wraps the single internal {@code rocket.StationRegistry} (it
 * duplicates no state) and projects each entry to an immutable {@link StationInfo}. Lets consumers such as
 * NeroLogistics replace a stubbed route provider. Everything outside {@code za.co.neroland.nerospace.api}
 * is internal and may change without notice.</p>
 *
 * <p><b>Privacy (POPIA/GDPR).</b> {@link StationInfo} omits the founder's UUID entirely. Ownership is
 * surfaced only as a per-player boolean via {@link #ownedBy}. Because this facade reads the live registry,
 * it automatically respects Core's {@code PlayerDataErasure} hook that Nerospace registers: once a player
 * is purged their station ownership is anonymised at the source, so {@link #ownedBy} then returns
 * {@code false} for them — no separate erasure path runs through this API.</p>
 */
public final class NerospaceStations {

    /**
     * Baseline number of logistics routes a station can anchor, exposed on every {@link StationInfo}. The
     * internal registry currently models no per-station capacity, so this is a stable uniform value;
     * should per-station capacity be modelled later, the {@link StationInfo#routeCapacity()} field stays —
     * only the value varies — so consumers need not change.
     */
    public static final int DEFAULT_ROUTE_CAPACITY = 8;

    private NerospaceStations() {
    }

    /** All founded stations, in founding order. The returned list and its elements are immutable. */
    public static List<StationInfo> all(MinecraftServer server) {
        if (server == null) {
            return List.of();
        }
        List<StationInfo> out = new ArrayList<>();
        for (StationEntry entry : StationRegistry.get(server).all()) {
            out.add(toInfo(entry));
        }
        return List.copyOf(out);
    }

    /** The station with the given id (slot), or empty if none. */
    public static Optional<StationInfo> byId(MinecraftServer server, int id) {
        if (server == null) {
            return Optional.empty();
        }
        StationEntry entry = StationRegistry.get(server).get(id);
        return entry == null ? Optional.empty() : Optional.of(toInfo(entry));
    }

    /**
     * All stations on a given planet. Stations only exist in the orbital {@link NerospacePlanets#STATION}
     * void, so any other planet yields an empty list.
     */
    public static List<StationInfo> byPlanet(MinecraftServer server, PlanetId planet) {
        return NerospacePlanets.STATION.equals(planet) ? all(server) : List.of();
    }

    /**
     * Whether the station with the given id is owned by {@code player}. Returns {@code false} for an
     * unknown station or an unowned (legacy / anonymised) one — including after a POPIA/GDPR erasure of
     * that player. Prefer this over reading raw owner UUIDs.
     */
    public static boolean ownedBy(MinecraftServer server, int id, UUID player) {
        if (server == null || player == null) {
            return false;
        }
        StationEntry entry = StationRegistry.get(server).get(id);
        return entry != null && !entry.owner().isEmpty() && entry.owner().equals(player.toString());
    }

    /** Convenience overload of {@link #ownedBy(MinecraftServer, int, UUID)} for a {@link ServerPlayer}. */
    public static boolean ownedBy(int id, ServerPlayer player) {
        return player != null && ownedBy(player.level().getServer(), id, player.getUUID());
    }

    private static StationInfo toInfo(StationEntry entry) {
        return new StationInfo(entry.slot(), entry.name(), NerospacePlanets.STATION,
                entry.center(), DEFAULT_ROUTE_CAPACITY);
    }
}
