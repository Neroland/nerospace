package za.co.neroland.nerospace.route;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.route.PadHandle;
import za.co.neroland.nerospace.api.route.ScheduleMode;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.rocket.StationStructure;

/**
 * The server-authoritative cargo-route store: Cargo Pads, the routes between them and every flight in the
 * air (plus recently completed ones, for the retention window). One {@link SavedData} on the overworld,
 * fetched through Neroland Core's {@link SavedDataRecovery} guard — Nerospace's sixth guarded store.
 * Schema in {@code docs/CARGO-ROCKETS.md} §3.
 *
 * <p><b>Threading.</b> Server thread only. Every mutator calls {@link #setDirty()}.</p>
 *
 * <p><b>Privacy (POPIA/GDPR).</b> The owner UUID on pads, routes and flights and the UUIDs on a pad's
 * access list are the only personal data. {@link #forgetPlayer} removes the player's routes, anonymises
 * their pads (the station rule — the block stays as shared world content), strips them from every access
 * list and anonymises their live flights, whose cargo keeps flying: items are not personal data, the
 * ownership record is. The registry never logs a UUID; log lines and breadcrumbs carry ids only.</p>
 *
 * <p><b>Never loads anything.</b> Every query here is a map lookup. Dimension and chunk liveness is
 * checked by {@link CargoFlights} with {@code server.getLevel} / {@code level.hasChunk} — never
 * {@code getChunk}.</p>
 */
public final class RouteRegistry extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "cargo_routes");
    /** Stable, non-identifying label for logs, telemetry and the recovery backup file name. */
    public static final String RECOVERY_NAME = "nerospace:cargo_routes";

    public static final SavedDataType<RouteRegistry> TYPE = new SavedDataType<>(ID, RouteRegistry::new, codec(), null);

    /** Insertion-ordered by id. */
    private final Map<Integer, PadRecord> pads = new LinkedHashMap<>();
    private final Map<Integer, RouteRecord> routes = new LinkedHashMap<>();
    private final Map<Integer, FlightRecord> flights = new LinkedHashMap<>();
    private int nextPadId = 1;
    private int nextRouteId = 1;
    private int nextFlightId = 1;

    public RouteRegistry() {
    }

    // --- Access ------------------------------------------------------------------------------

    /** The one registry, on the overworld so it is always loaded. Guarded by Core's recovery ladder. */
    public static RouteRegistry get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), TYPE, RouteRegistry::new, RECOVERY_NAME);
    }

    /** Push the current state into the last-known-good backup immediately (erasure path). */
    public static void backupNow(MinecraftServer server, RouteRegistry registry) {
        SavedDataRecovery.backupNow(server.overworld(), TYPE, registry, RECOVERY_NAME);
    }

    /** The codec, exposed for the plain-JVM round-trip tests. */
    public static Codec<RouteRegistry> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                PadRecord.CODEC.listOf().optionalFieldOf("pads", List.of()).forGetter(r -> new ArrayList<>(r.pads.values())),
                RouteRecord.CODEC.listOf().optionalFieldOf("routes", List.of()).forGetter(r -> new ArrayList<>(r.routes.values())),
                FlightRecord.CODEC.listOf().optionalFieldOf("flights", List.of()).forGetter(r -> new ArrayList<>(r.flights.values())),
                Codec.INT.optionalFieldOf("next_pad_id", 1).forGetter(r -> r.nextPadId),
                Codec.INT.optionalFieldOf("next_route_id", 1).forGetter(r -> r.nextRouteId),
                Codec.INT.optionalFieldOf("next_flight_id", 1).forGetter(r -> r.nextFlightId)
        ).apply(inst, RouteRegistry::fromData));
    }

    private static RouteRegistry fromData(List<PadRecord> pads, List<RouteRecord> routes, List<FlightRecord> flights,
            Integer nextPad, Integer nextRoute, Integer nextFlight) {
        RouteRegistry r = new RouteRegistry();
        for (PadRecord pad : pads) {
            r.pads.put(pad.id(), pad);
        }
        for (RouteRecord route : routes) {
            r.routes.put(route.id(), route);
        }
        for (FlightRecord flight : flights) {
            r.flights.put(flight.id(), flight);
        }
        r.nextPadId = Math.max(nextPad.intValue(), 1);
        r.nextRouteId = Math.max(nextRoute.intValue(), 1);
        r.nextFlightId = Math.max(nextFlight.intValue(), 1);
        return r;
    }

    static String dimId(ResourceKey<Level> dim) {
        return dim.identifier().toString();
    }

    static ResourceKey<Level> dimKey(String id) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(id));
    }

    static String uuidString(@Nullable UUID uuid) {
        return uuid == null ? "" : uuid.toString();
    }

    // --- Pads --------------------------------------------------------------------------------

    /** The pad record at exactly {@code (dim, pos)}, or {@code null}. */
    @Nullable
    public PadRecord padAt(ResourceKey<Level> dim, BlockPos pos) {
        String d = dimId(dim);
        for (PadRecord pad : this.pads.values()) {
            if (pad.dim().equals(d) && pad.pos().equals(pos)) {
                return pad;
            }
        }
        return null;
    }

    /** A placed Cargo Pad by id (never a station), or {@code null}. */
    @Nullable
    public PadRecord pad(int id) {
        return this.pads.get(id);
    }

    /** Every placed pad, in registration order. Internal — callers that answer players use {@link #padsVisibleTo}. */
    public List<PadRecord> allPads() {
        return List.copyOf(this.pads.values());
    }

    public int padCount() {
        return this.pads.size();
    }

    /**
     * Registers the pad at {@code (dim, pos)} or returns the existing record there. {@code owner} is
     * adopted only for a brand-new record (or one that lost its owner and is being placed again): a pad
     * re-registering after a block-entity load passes {@code null} and never overrides the store — the
     * store, not the block, is the owner of record, which is what makes erasure stick.
     *
     * @return the record, or {@code null} when the {@code cargoMaxPads} cap is reached for a new pad
     */
    @Nullable
    public PadRecord registerPad(ResourceKey<Level> dim, BlockPos pos, @Nullable UUID owner, @Nullable String name) {
        PadRecord existing = padAt(dim, pos);
        if (existing != null) {
            if (owner != null && existing.owner().isEmpty()) {
                PadRecord claimed = existing.withOwner(uuidString(owner));
                this.pads.put(claimed.id(), claimed);
                setDirty();
                return claimed;
            }
            return existing;
        }
        if (this.pads.size() >= NerospaceConfig.cargoMaxPads()) {
            return null;
        }
        int id = this.nextPadId++;
        String label = name == null || name.isBlank() ? "Cargo Pad " + id : name;
        PadRecord pad = new PadRecord(id, label, dimId(dim), pos.immutable(), uuidString(owner), false, List.of());
        this.pads.put(id, pad);
        setDirty();
        RouteEventDispatch.padRegistered(pad.view());
        return pad;
    }

    /** Removes a pad and every route touching it. Flights bound for it hold, then crate. */
    @Nullable
    public PadRecord unregisterPad(int id) {
        PadRecord removed = this.pads.remove(id);
        if (removed == null) {
            return null;
        }
        this.routes.values().removeIf(route -> route.originPadId() == id || route.destinationPadId() == id);
        setDirty();
        RouteEventDispatch.padUnregistered(removed.view());
        return removed;
    }

    /** Lets a player claim an unowned pad (after a store recovery). @return whether it was claimed. */
    public boolean claimPad(int id, UUID player) {
        PadRecord pad = this.pads.get(id);
        if (pad == null || player == null || !pad.owner().isEmpty()) {
            return false;
        }
        this.pads.put(id, pad.withOwner(player.toString()));
        setDirty();
        return true;
    }

    public boolean renamePad(int id, String name) {
        PadRecord pad = this.pads.get(id);
        if (pad == null || name == null || name.isBlank()) {
            return false;
        }
        this.pads.put(id, pad.withName(name.strip()));
        setDirty();
        return true;
    }

    public boolean setPadPublic(int id, boolean isPublic) {
        PadRecord pad = this.pads.get(id);
        if (pad == null) {
            return false;
        }
        this.pads.put(id, pad.withPublic(isPublic));
        setDirty();
        return true;
    }

    /** Adds {@code player} to the access list (idempotent, capped so a list cannot bloat the save). */
    public boolean grantAccess(int id, UUID player) {
        PadRecord pad = this.pads.get(id);
        if (pad == null || player == null) {
            return false;
        }
        String key = player.toString();
        if (pad.access().contains(key) || pad.access().size() >= PadRecord.MAX_ACCESS) {
            return false;
        }
        List<String> access = new ArrayList<>(pad.access());
        access.add(key);
        this.pads.put(id, pad.withAccess(access));
        setDirty();
        return true;
    }

    public boolean revokeAccess(int id, UUID player) {
        PadRecord pad = this.pads.get(id);
        if (pad == null || player == null || !pad.access().contains(player.toString())) {
            return false;
        }
        List<String> access = new ArrayList<>(pad.access());
        access.remove(player.toString());
        this.pads.put(id, pad.withAccess(access));
        setDirty();
        return true;
    }

    // --- Station endpoints -------------------------------------------------------------------

    /** Whether {@code padId} names a founded station rather than a placed pad. */
    public static boolean isStationId(int padId) {
        return padId < 0;
    }

    public static int stationPadId(int slot) {
        return -(slot + 1);
    }

    public static int stationSlot(int padId) {
        return -padId - 1;
    }

    /**
     * Resolves any endpoint id — a placed pad or a station — to a view, or {@code null} if unknown. The
     * station view is synthesised from {@link StationRegistry} on every call so it always reflects the
     * live founder record (an erased founder reads as unowned at once).
     */
    @Nullable
    public PadHandle resolve(MinecraftServer server, int padId) {
        if (!isStationId(padId)) {
            PadRecord pad = this.pads.get(padId);
            return pad == null ? null : pad.view();
        }
        StationRegistry.StationEntry entry = StationRegistry.get(server).get(stationSlot(padId));
        return entry == null ? null : StationPad.of(entry);
    }

    /** Where a flight to {@code padId} lands, or {@code null} if the endpoint is gone. */
    @Nullable
    public Endpoint endpoint(MinecraftServer server, int padId) {
        if (!isStationId(padId)) {
            PadRecord pad = this.pads.get(padId);
            return pad == null ? null : new Endpoint(dimKey(pad.dim()), pad.pos(), false);
        }
        StationRegistry.StationEntry entry = StationRegistry.get(server).get(stationSlot(padId));
        return entry == null ? null
                : new Endpoint(ModDimensions.STATION_LEVEL, StationStructure.padCenter(entry.center()), true);
    }

    /** A resolved landing place: dimension + position, and whether it is a station (search radius applies). */
    public record Endpoint(ResourceKey<Level> dimension, BlockPos position, boolean station) {
    }

    /**
     * The endpoints {@code player} may route to: pads they own, are listed on, or that are public, plus
     * stations they may manage. Never a server-wide roster.
     */
    public List<PadHandle> padsVisibleTo(MinecraftServer server, @Nullable UUID player) {
        if (player == null) {
            return List.of();
        }
        List<PadHandle> out = new ArrayList<>();
        for (PadRecord pad : this.pads.values()) {
            if (pad.accessibleBy(player)) {
                out.add(pad.view());
            }
        }
        for (StationRegistry.StationEntry entry : StationRegistry.get(server).all()) {
            PadHandle station = StationPad.of(entry);
            if (station.accessibleBy(player)) {
                out.add(station);
            }
        }
        return List.copyOf(out);
    }

    // --- Routes ------------------------------------------------------------------------------

    /** The route configured on {@code originPadId} (at most one per origin), or {@code null}. */
    @Nullable
    public RouteRecord routeForOrigin(int originPadId) {
        for (RouteRecord route : this.routes.values()) {
            if (route.originPadId() == originPadId) {
                return route;
            }
        }
        return null;
    }

    @Nullable
    public RouteRecord route(int id) {
        return this.routes.get(id);
    }

    public List<RouteRecord> routesOwnedBy(@Nullable UUID player) {
        if (player == null) {
            return List.of();
        }
        String key = player.toString();
        List<RouteRecord> out = new ArrayList<>();
        for (RouteRecord route : this.routes.values()) {
            if (key.equals(route.owner())) {
                out.add(route);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Points {@code originPadId} at {@code destinationPadId}, keeping the schedule if a route already
     * existed for that origin. Owner = the origin pad's owner at the time (routes follow the pad).
     */
    @Nullable
    public RouteRecord setRoute(int originPadId, int destinationPadId) {
        PadRecord origin = this.pads.get(originPadId);
        if (origin == null || originPadId == destinationPadId) {
            return null;
        }
        RouteRecord existing = routeForOrigin(originPadId);
        RouteRecord route = existing != null
                ? existing.withDestination(destinationPadId).withOwner(origin.owner())
                : new RouteRecord(this.nextRouteId++, originPadId, destinationPadId, origin.owner(),
                        ScheduleMode.MANUAL, RouteRecord.DEFAULT_INTERVAL_TICKS, false);
        this.routes.put(route.id(), route);
        setDirty();
        return route;
    }

    public void clearRoute(int originPadId) {
        if (this.routes.values().removeIf(route -> route.originPadId() == originPadId)) {
            setDirty();
        }
    }

    @Nullable
    public RouteRecord updateSchedule(int routeId, ScheduleMode mode, int intervalTicks) {
        RouteRecord route = this.routes.get(routeId);
        if (route == null) {
            return null;
        }
        RouteRecord updated = route.withSchedule(mode, intervalTicks);
        this.routes.put(routeId, updated);
        setDirty();
        return updated;
    }

    @Nullable
    public RouteRecord setReturnEmpty(int routeId, boolean returnEmpty) {
        RouteRecord route = this.routes.get(routeId);
        if (route == null) {
            return null;
        }
        RouteRecord updated = route.withReturnEmpty(returnEmpty);
        this.routes.put(routeId, updated);
        setDirty();
        return updated;
    }

    // --- Flights -----------------------------------------------------------------------------

    @Nullable
    public FlightRecord flight(int id) {
        return this.flights.get(id);
    }

    /** Every flight record, oldest first (live and retained). */
    public List<FlightRecord> allFlights() {
        return List.copyOf(this.flights.values());
    }

    /** Flights that are not yet terminal, oldest first. */
    public List<FlightRecord> liveFlights() {
        List<FlightRecord> out = new ArrayList<>();
        for (FlightRecord flight : this.flights.values()) {
            if (!flight.state().isTerminal()) {
                out.add(flight);
            }
        }
        return out;
    }

    public List<FlightRecord> flightsOwnedBy(@Nullable UUID player) {
        if (player == null) {
            return List.of();
        }
        String key = player.toString();
        List<FlightRecord> out = new ArrayList<>();
        for (FlightRecord flight : this.flights.values()) {
            if (key.equals(flight.owner())) {
                out.add(flight);
            }
        }
        return List.copyOf(out);
    }

    public int liveFlightsOwnedBy(@Nullable UUID player) {
        if (player == null) {
            return 0;
        }
        String key = player.toString();
        int n = 0;
        for (FlightRecord flight : this.flights.values()) {
            if (!flight.state().isTerminal() && key.equals(flight.owner())) {
                n++;
            }
        }
        return n;
    }

    /** Whether any live flight is bound for {@code padId} — the pad GUI shows it as "inbound". */
    public int inboundCount(int padId) {
        int n = 0;
        for (FlightRecord flight : this.flights.values()) {
            if (!flight.state().isTerminal() && flight.destinationPadId() == padId) {
                n++;
            }
        }
        return n;
    }

    /**
     * Records a departure. {@code items} are copied; the caller has already removed them from the pad and
     * drained the fuel. Flags the store dirty so the flight survives a restart from this tick on.
     */
    public FlightRecord createFlight(int routeId, int originPadId, int destinationPadId, String owner,
            boolean returnLeg, Endpoint origin, Endpoint destination, int fuel, List<ItemStack> items,
            long departedAt, int travelTicks) {
        int id = this.nextFlightId++;
        FlightRecord flight = new FlightRecord(id, routeId, originPadId, destinationPadId,
                owner == null ? "" : owner, returnLeg, dimId(origin.dimension()), origin.position(),
                dimId(destination.dimension()), destination.position(), fuel, items, departedAt,
                departedAt + Math.max(1, travelTicks));
        this.flights.put(id, flight);
        setDirty();
        return flight;
    }

    /** Mutators on a {@link FlightRecord} call this so the change is persisted. */
    void flightChanged() {
        setDirty();
    }

    /** Drops terminal flights whose completion is older than the retention window. */
    public int prune(long now) {
        long retention = NerospaceConfig.cargoFlightRetentionTicks();
        int removed = 0;
        Iterator<FlightRecord> it = this.flights.values().iterator();
        while (it.hasNext()) {
            FlightRecord flight = it.next();
            if (flight.state().isTerminal() && flight.completedAt() >= 0 && now - flight.completedAt() > retention) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    // --- Erasure -----------------------------------------------------------------------------

    /**
     * POPIA/GDPR erasure for one player. Routes they own are removed (a route is pure configuration and
     * its pad can be re-pointed by whoever claims it); pads they own are anonymised, not removed (the
     * station rule — shared world content stays); they are dropped from every access list; live flights
     * they own are anonymised and keep flying, retained ones too. Never logs the UUID.
     *
     * @return how many records were changed
     */
    public int forgetPlayer(UUID uuid) {
        if (uuid == null) {
            return 0;
        }
        String key = uuid.toString();
        int changed = 0;
        for (Map.Entry<Integer, PadRecord> e : this.pads.entrySet()) {
            PadRecord pad = e.getValue();
            boolean owns = key.equals(pad.owner());
            boolean listed = pad.access().contains(key);
            if (owns || listed) {
                List<String> access = new ArrayList<>(pad.access());
                access.remove(key);
                e.setValue(new PadRecord(pad.id(), pad.name(), pad.dim(), pad.pos(),
                        owns ? "" : pad.owner(), pad.isPublic(), access));
                changed++;
            }
        }
        Iterator<RouteRecord> routeIt = this.routes.values().iterator();
        while (routeIt.hasNext()) {
            if (key.equals(routeIt.next().owner())) {
                routeIt.remove();
                changed++;
            }
        }
        for (FlightRecord flight : this.flights.values()) {
            if (key.equals(flight.owner())) {
                flight.anonymise();
                changed++;
            }
        }
        if (changed > 0) {
            setDirty();
        }
        return changed;
    }

    /** Whether the store still holds anything keyed by {@code uuid} — the erasure-conformance probe. */
    public boolean retainsData(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        String key = uuid.toString();
        for (PadRecord pad : this.pads.values()) {
            if (key.equals(pad.owner()) || pad.access().contains(key)) {
                return true;
            }
        }
        for (RouteRecord route : this.routes.values()) {
            if (key.equals(route.owner())) {
                return true;
            }
        }
        for (FlightRecord flight : this.flights.values()) {
            if (key.equals(flight.owner())) {
                return true;
            }
        }
        return false;
    }
}
