package za.co.neroland.nerospace.route;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.route.FlightState;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.progression.StarGuideGrants;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;

/**
 * The flight state machine, driven once per server tick from every loader's server-tick hook (beside the
 * meteor / oxygen / gravity drivers). Arrivals are checked every {@value #ARRIVAL_INTERVAL} ticks, unloading
 * moves one stack every {@value #UNLOAD_INTERVAL} ticks so pipes keep up, retention pruning runs every
 * {@value #PRUNE_INTERVAL} ticks. See {@code docs/CARGO-ROCKETS.md} §6 for the lifecycle.
 *
 * <p><b>Never loads anything.</b> A destination whose dimension or chunk is not loaded parks the flight in
 * {@link FlightState#AWAITING_CHUNK}; the check is {@code server.getLevel} + {@code level.hasChunk}, never
 * {@code getChunk}. <b>Never deletes cargo.</b> The only exit for undeliverable items is a Cargo Crate at
 * the destination position.</p>
 *
 * <p><b>Privacy:</b> log lines and breadcrumbs carry flight ids only.</p>
 */
public final class CargoFlights {

    private static final int ARRIVAL_INTERVAL = 20;
    private static final int UNLOAD_INTERVAL = 2;
    private static final int PRUNE_INTERVAL = 6_000;
    /** Station endpoints: a Cargo Pad within this many blocks of the station's landing pad counts. */
    private static final int STATION_SEARCH_RADIUS = 6;
    /** The Star Guide advancement granted on a player's first delivered flight. */
    private static final String FIRST_FREIGHT_ADVANCEMENT = "guide/first_freight";

    private static long tick;

    private CargoFlights() {
    }

    /** Per-server tick entry point. Cheap when nothing is flying. */
    public static void tick(MinecraftServer server) {
        tick++;
        RouteRegistry registry = RouteRegistry.get(server);
        List<FlightRecord> live = registry.liveFlights();
        if (live.isEmpty()) {
            if (tick % PRUNE_INTERVAL == 0) {
                registry.prune(server.overworld().getGameTime());
            }
            return;
        }
        long now = server.overworld().getGameTime();
        boolean arrivals = tick % ARRIVAL_INTERVAL == 0;
        boolean unloads = tick % UNLOAD_INTERVAL == 0;
        for (FlightRecord flight : live) {
            try {
                switch (flight.state()) {
                    case IN_FLIGHT -> {
                        if (arrivals && now >= flight.arrivesAt()) {
                            tryArrive(server, registry, flight, now);
                        }
                    }
                    case AWAITING_CHUNK -> {
                        if (arrivals) {
                            tryArrive(server, registry, flight, now);
                        }
                    }
                    case HOLDING -> {
                        if (arrivals && (now >= flight.nextRetryAt() || flight.cancelRequested())) {
                            tryArrive(server, registry, flight, now);
                        }
                    }
                    case UNLOADING -> {
                        if (unloads) {
                            unloadStep(server, registry, flight, now);
                        }
                    }
                    default -> {
                    }
                }
            } catch (RuntimeException e) {
                // One bad flight must not stall the others; it retries on the next pass. Id only.
                NerospaceCommon.LOGGER.warn("[Nerospace] Cargo flight {} tick failed; will retry.", flight.id(), e);
                NerospaceTelemetry.captureHandledException(e, "cargo_flight", "tick");
            }
        }
        if (tick % PRUNE_INTERVAL == 0) {
            registry.prune(now);
        }
    }

    // --- Arrival -----------------------------------------------------------------------------

    private static void tryArrive(MinecraftServer server, RouteRegistry registry, FlightRecord flight, long now) {
        ServerLevel level = server.getLevel(RouteRegistry.dimKey(flight.destDim()));
        BlockPos pos = flight.destPos();
        if (level == null || !level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            if (flight.state() != FlightState.AWAITING_CHUNK) {
                flight.setState(FlightState.AWAITING_CHUNK);
                registry.flightChanged();
            }
            return;
        }
        if (flight.cancelRequested()) {
            crate(level, pos, registry, flight, now, "cancelled");
            return;
        }
        boolean station = RouteRegistry.isStationId(flight.destinationPadId());
        CargoPadBlockEntity pad = findPad(level, pos, station ? STATION_SEARCH_RADIUS : 0);
        if (pad == null) {
            hold(level, pos, registry, flight, now);
            return;
        }
        if (!flight.landed()) {
            if (pad.isOccupied()) {
                hold(level, pos, registry, flight, now);
                return;
            }
            CargoRocketEntity rocket = CargoRocketEntity.standOn(level, pad.getBlockPos());
            if (flight.fuel() > 0) {
                rocket.addFuel(flight.fuel());
            }
            level.addFreshEntity(rocket);
            flight.markLanded();
            arrivalEffect(level, pad.getBlockPos());
            NerospaceTelemetry.breadcrumb("cargo", "land flight=" + flight.id());
        }
        flight.clearHold();
        if (flight.items().isEmpty()) {
            finishDelivery(server, registry, flight, now, pad);
        } else {
            flight.setState(FlightState.UNLOADING);
        }
        registry.flightChanged();
    }

    private static void unloadStep(MinecraftServer server, RouteRegistry registry, FlightRecord flight, long now) {
        ServerLevel level = server.getLevel(RouteRegistry.dimKey(flight.destDim()));
        BlockPos pos = flight.destPos();
        if (level == null || !level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            flight.setState(FlightState.AWAITING_CHUNK);
            registry.flightChanged();
            return;
        }
        boolean station = RouteRegistry.isStationId(flight.destinationPadId());
        CargoPadBlockEntity pad = findPad(level, pos, station ? STATION_SEARCH_RADIUS : 0);
        if (pad == null) {
            hold(level, pos, registry, flight, now);
            return;
        }
        List<ItemStack> items = flight.items();
        if (!items.isEmpty()) {
            ItemStack next = items.get(0);
            ItemStack rest = pad.acceptCargo(next);
            if (!rest.isEmpty() && rest.getCount() == next.getCount()) {
                // The hold is full: hold (the rocket stays docked) and retry; the timeout crates the rest.
                hold(level, pos, registry, flight, now);
                return;
            }
            if (rest.isEmpty()) {
                items.remove(0);
            } else {
                items.set(0, rest);
            }
            registry.flightChanged();
        }
        if (items.isEmpty()) {
            finishDelivery(server, registry, flight, now, pad);
            registry.flightChanged();
        }
    }

    private static void finishDelivery(MinecraftServer server, RouteRegistry registry, FlightRecord flight, long now,
            CargoPadBlockEntity pad) {
        flight.complete(FlightState.DELIVERED, now);
        NerospaceTelemetry.breadcrumb("cargo", "delivered flight=" + flight.id());
        RouteEventDispatch.arrived(flight.view());
        grantFirstFreight(server, flight);

        RouteRecord route = flight.routeId() >= 0 ? registry.route(flight.routeId()) : null;
        boolean returnEmpty = route != null && route.returnEmpty();
        if (returnEmpty && !flight.returnLeg()) {
            // Fly the empty hull home: fuel was paid at launch, the timer is priced from here. If the origin
            // pad vanished while we were away the hull simply stays on this pad rather than being lost.
            RouteRegistry.Endpoint back = registry.endpoint(server, flight.originPadId());
            RouteRegistry.Endpoint here = registry.endpoint(server, flight.destinationPadId());
            CargoRocketEntity rocket = pad.dockedRocket();
            if (back != null && here != null && rocket != null) {
                int carried = rocket.getFuel();
                rocket.discard();
                double gO = CargoFormulas.gravityOf(here.dimension());
                double gD = CargoFormulas.gravityOf(back.dimension());
                int ticks = CargoFormulas.travelTicks(gO, gD, here.dimension().equals(back.dimension()));
                FlightRecord returnFlight = registry.createFlight(flight.routeId(), flight.destinationPadId(),
                        flight.originPadId(), flight.owner(), true, here, back, carried, List.of(), now, ticks);
                NerospaceTelemetry.breadcrumb("cargo", "return flight=" + returnFlight.id());
                RouteEventDispatch.departed(returnFlight.view());
            }
        }
    }

    // --- Holding / crating ------------------------------------------------------------------

    private static void hold(ServerLevel level, BlockPos pos, RouteRegistry registry, FlightRecord flight, long now) {
        boolean first = flight.holdSince() < 0;
        if (!first && now - flight.holdSince() >= NerospaceConfig.cargoHoldTimeoutTicks()) {
            crate(level, pos, registry, flight, now, "timeout");
            return;
        }
        flight.markHeld(now, now + NerospaceConfig.cargoHoldRetryTicks());
        registry.flightChanged();
        if (first) {
            NerospaceTelemetry.breadcrumb("cargo", "hold flight=" + flight.id());
            RouteEventDispatch.held(flight.view());
        }
    }

    /** The only exit for undeliverable cargo: a crate item at the destination. Never deletes anything. */
    private static void crate(ServerLevel level, BlockPos pos, RouteRegistry registry, FlightRecord flight, long now,
            String why) {
        ItemStack crate = CargoCrateItem.crate(ModItems.CARGO_CRATE.get(), flight.items());
        if (!crate.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, crate);
        }
        flight.items().clear();
        flight.complete(FlightState.DROPPED, now);
        registry.flightChanged();
        NerospaceTelemetry.breadcrumb("cargo", "dropped flight=" + flight.id() + " why=" + why);
        RouteEventDispatch.dropped(flight.view());
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** The Cargo Pad at {@code pos}, or (for stations) the nearest one within {@code radius} blocks. */
    @Nullable
    static CargoPadBlockEntity findPad(ServerLevel level, BlockPos pos, int radius) {
        if (level.getBlockEntity(pos) instanceof CargoPadBlockEntity direct) {
            return direct;
        }
        if (radius <= 0) {
            return null;
        }
        CargoPadBlockEntity best = null;
        double bestSq = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (!level.hasChunk(p.getX() >> 4, p.getZ() >> 4)) {
                        continue;
                    }
                    if (level.getBlockEntity(p) instanceof CargoPadBlockEntity pad) {
                        double sq = p.distSqr(pos);
                        if (sq < bestSq) {
                            bestSq = sq;
                            best = pad;
                        }
                    }
                }
            }
        }
        return best;
    }

    private static void arrivalEffect(ServerLevel level, BlockPos pad) {
        double x = pad.getX() + 0.5D;
        double y = pad.getY() + 0.4D;
        double z = pad.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 24, 0.8D, 0.2D, 0.8D, 0.02D);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 12, 0.5D, 0.1D, 0.5D, 0.01D);
        level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 2.0F, 0.5F);
        level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 0.7F);
    }

    /** First delivered flight = the Star Guide "Freight" chapter's last step. Owner must be online; UUID never logged. */
    private static void grantFirstFreight(MinecraftServer server, FlightRecord flight) {
        if (flight.owner().isEmpty() || flight.returnLeg()) {
            return;
        }
        UUID owner = CargoPadBlockEntity.parseUuid(flight.owner());
        ServerPlayer player = owner == null ? null : server.getPlayerList().getPlayer(owner);
        if (player != null) {
            StarGuideGrants.grant(player, FIRST_FREIGHT_ADVANCEMENT);
        }
    }
}
