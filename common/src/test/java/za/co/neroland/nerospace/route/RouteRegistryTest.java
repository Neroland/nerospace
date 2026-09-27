package za.co.neroland.nerospace.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.data.ErasureConformance;
import za.co.neroland.nerolandcore.data.PlayerDataEraser;
import za.co.neroland.nerolandcore.data.PlayerDataErasure;

import za.co.neroland.nerospace.api.route.FlightState;
import za.co.neroland.nerospace.api.route.ScheduleMode;
import za.co.neroland.nerospace.config.NerospaceConfig;

/**
 * The cargo-route store against an in-memory instance: registration, routes, flights, the codec round trip,
 * retention pruning and — the one that matters for POPIA/GDPR — erasure, verified with Core's
 * {@link ErasureConformance} harness. No server, no item registries (manifests stay empty here).
 */
class RouteRegistryTest {

    private static final ResourceKey<Level> OVERWORLD = RouteRegistry.dimKey("minecraft:overworld");
    private static final ResourceKey<Level> GREENXERTZ = RouteRegistry.dimKey("nerospace:greenxertz");

    private static RouteRegistry roundTrip(RouteRegistry registry) {
        JsonElement encoded = RouteRegistry.codec().encodeStart(JsonOps.INSTANCE, registry)
                .getOrThrow(error -> new AssertionError("encode failed: " + error));
        return RouteRegistry.codec().parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(error -> new AssertionError("decode failed: " + error));
    }

    private static RouteRegistry.Endpoint at(ResourceKey<Level> dim, BlockPos pos) {
        return new RouteRegistry.Endpoint(dim, pos, false);
    }

    @Test
    @DisplayName("pads register once per position, keep their id, and the store never overrides an owner")
    void padRegistration() {
        RouteRegistry registry = new RouteRegistry();
        UUID owner = UUID.randomUUID();
        PadRecord pad = registry.registerPad(OVERWORLD, new BlockPos(1, 64, 1), owner, "Home Dock");
        assertNotNull(pad);
        assertEquals("Home Dock", pad.name());
        assertTrue(pad.ownedBy(owner));

        PadRecord again = registry.registerPad(OVERWORLD, new BlockPos(1, 64, 1), null, null);
        assertEquals(pad.id(), again.id(), "re-registering after a load returns the same record");
        assertTrue(again.ownedBy(owner), "a null owner never clears the stored owner");

        PadRecord stranger = registry.registerPad(OVERWORLD, new BlockPos(1, 64, 1), UUID.randomUUID(), null);
        assertTrue(stranger.ownedBy(owner), "an owned pad is not re-claimed by another placer");

        assertEquals(1, registry.padCount());
        assertEquals("Cargo Pad 2", registry.registerPad(GREENXERTZ, new BlockPos(5, 70, 5), null, "  ").name());
    }

    @Test
    @DisplayName("access: owner, access list and public flag; unowned pads are not routable by strangers")
    void access() {
        RouteRegistry registry = new RouteRegistry();
        UUID owner = UUID.randomUUID();
        UUID friend = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        PadRecord pad = registry.registerPad(OVERWORLD, new BlockPos(0, 64, 0), owner, null);

        assertTrue(pad.view().accessibleBy(owner));
        assertFalse(pad.view().accessibleBy(stranger));
        assertTrue(registry.grantAccess(pad.id(), friend));
        assertFalse(registry.grantAccess(pad.id(), friend), "idempotent");
        assertTrue(registry.pad(pad.id()).view().accessibleBy(friend));
        assertTrue(registry.setPadPublic(pad.id(), true));
        assertTrue(registry.pad(pad.id()).view().accessibleBy(stranger));
        assertTrue(registry.revokeAccess(pad.id(), friend));
        assertFalse(registry.pad(pad.id()).view().ownedBy(friend));

        PadRecord unowned = registry.registerPad(GREENXERTZ, new BlockPos(9, 64, 9), null, null);
        assertFalse(unowned.view().accessibleBy(stranger), "unowned pads wait to be claimed");
        assertTrue(registry.claimPad(unowned.id(), stranger));
        assertTrue(registry.pad(unowned.id()).view().ownedBy(stranger));
        assertFalse(registry.claimPad(unowned.id(), owner), "already claimed");
    }

    @Test
    @DisplayName("one route per origin; re-pointing keeps the schedule; removing a pad removes its routes")
    void routes() {
        RouteRegistry registry = new RouteRegistry();
        UUID owner = UUID.randomUUID();
        PadRecord a = registry.registerPad(OVERWORLD, new BlockPos(0, 64, 0), owner, "A");
        PadRecord b = registry.registerPad(GREENXERTZ, new BlockPos(0, 64, 0), owner, "B");
        PadRecord c = registry.registerPad(GREENXERTZ, new BlockPos(20, 64, 0), owner, "C");

        assertNull(registry.setRoute(a.id(), a.id()), "a pad cannot route to itself");
        RouteRecord route = registry.setRoute(a.id(), b.id());
        assertNotNull(route);
        assertTrue(route.ownedBy(owner));
        registry.updateSchedule(route.id(), ScheduleMode.EVERY_INTERVAL, 100);
        assertEquals(RouteRecord.MIN_INTERVAL_TICKS, registry.route(route.id()).intervalTicks(), "interval is clamped");

        RouteRecord repointed = registry.setRoute(a.id(), c.id());
        assertEquals(route.id(), repointed.id());
        assertEquals(ScheduleMode.EVERY_INTERVAL, repointed.mode(), "schedule survives a re-point");
        assertEquals(1, registry.routesOwnedBy(owner).size());

        registry.unregisterPad(c.id());
        assertNull(registry.routeForOrigin(a.id()), "routes touching a removed pad are gone");
        assertEquals(2, registry.padCount());
    }

    @Test
    @DisplayName("station endpoint ids are negative and invertible")
    void stationIds() {
        for (int slot = 0; slot < 5; slot++) {
            int id = RouteRegistry.stationPadId(slot);
            assertTrue(RouteRegistry.isStationId(id));
            assertEquals(slot, RouteRegistry.stationSlot(id));
        }
        assertFalse(RouteRegistry.isStationId(1));
    }

    @Test
    @DisplayName("flights survive a codec round trip with their state and timers")
    void flightRoundTrip() {
        RouteRegistry registry = new RouteRegistry();
        UUID owner = UUID.randomUUID();
        PadRecord a = registry.registerPad(OVERWORLD, new BlockPos(0, 64, 0), owner, "A");
        PadRecord b = registry.registerPad(GREENXERTZ, new BlockPos(3, 65, 3), owner, "B");
        RouteRecord route = registry.setRoute(a.id(), b.id());
        FlightRecord flight = registry.createFlight(route.id(), a.id(), b.id(), owner.toString(), false,
                at(OVERWORLD, a.pos()), at(GREENXERTZ, b.pos()), 250, List.of(), 1_000L, 2_400);
        flight.markHeld(3_400L, 4_000L);

        RouteRegistry reloaded = roundTrip(registry);
        FlightRecord back = reloaded.flight(flight.id());
        assertNotNull(back);
        assertEquals(FlightState.HOLDING, back.state());
        assertEquals(1_000L, back.departedAt());
        assertEquals(3_400L, back.arrivesAt());
        assertEquals(3_400L, back.holdSince());
        assertEquals(4_000L, back.nextRetryAt());
        assertEquals(250, back.fuel());
        assertEquals("nerospace:greenxertz", back.destDim());
        assertEquals(new BlockPos(3, 65, 3), back.destPos());
        assertTrue(back.ownedBy(owner));
        assertEquals(1, reloaded.liveFlights().size());
        assertEquals(1, reloaded.inboundCount(b.id()));
        assertNotNull(reloaded.routeForOrigin(a.id()));
        assertEquals(route.id(), reloaded.routeForOrigin(a.id()).id());
    }

    @Test
    @DisplayName("retention: terminal flights are pruned after the configured window, live ones never")
    void retention() {
        RouteRegistry registry = new RouteRegistry();
        UUID owner = UUID.randomUUID();
        PadRecord a = registry.registerPad(OVERWORLD, new BlockPos(0, 64, 0), owner, "A");
        PadRecord b = registry.registerPad(OVERWORLD, new BlockPos(40, 64, 0), owner, "B");
        FlightRecord done = registry.createFlight(-1, a.id(), b.id(), owner.toString(), false,
                at(OVERWORLD, a.pos()), at(OVERWORLD, b.pos()), 0, List.of(), 0L, 100);
        done.complete(FlightState.DELIVERED, 100L);
        FlightRecord live = registry.createFlight(-1, a.id(), b.id(), owner.toString(), false,
                at(OVERWORLD, a.pos()), at(OVERWORLD, b.pos()), 0, List.of(), 0L, 100);

        long window = NerospaceConfig.cargoFlightRetentionTicks();
        assertEquals(0, registry.prune(100L + window), "not yet");
        assertEquals(1, registry.prune(101L + window));
        assertNull(registry.flight(done.id()));
        assertNotNull(registry.flight(live.id()), "a live flight is never pruned");
    }

    @Test
    @DisplayName("erasure removes routes, anonymises pads and flights, purges access lists — and passes Core's conformance harness")
    void erasure() {
        RouteRegistry registry = new RouteRegistry();
        UUID erased = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        PadRecord mine = registry.registerPad(OVERWORLD, new BlockPos(0, 64, 0), erased, "Mine");
        PadRecord theirs = registry.registerPad(GREENXERTZ, new BlockPos(0, 64, 0), other, "Theirs");
        registry.grantAccess(theirs.id(), erased);
        RouteRecord myRoute = registry.setRoute(mine.id(), theirs.id());
        RouteRecord theirRoute = registry.setRoute(theirs.id(), mine.id());
        FlightRecord myFlight = registry.createFlight(myRoute.id(), mine.id(), theirs.id(), erased.toString(), false,
                at(OVERWORLD, mine.pos()), at(GREENXERTZ, theirs.pos()), 0, List.of(), 0L, 600);

        PlayerDataEraser eraser = (server, uuid) -> registry.forgetPlayer(uuid);
        PlayerDataErasure.register(eraser);
        try {
            ErasureConformance.create()
                    .probe("nerospace:cargo_routes", registry::retainsData)
                    .verify(null, erased);
        } finally {
            PlayerDataErasure.unregister(eraser);
        }

        assertFalse(registry.retainsData(erased));
        assertNull(registry.routeForOrigin(mine.id()), "the erased player's route is gone");
        assertNotNull(registry.route(theirRoute.id()), "someone else's route is untouched");
        assertFalse(registry.pad(mine.id()).view().ownedBy(erased), "pad anonymised, not deleted");
        assertNotNull(registry.pad(mine.id()));
        assertFalse(registry.pad(theirs.id()).view().accessibleBy(erased), "dropped from the access list");
        assertTrue(registry.pad(theirs.id()).view().ownedBy(other));
        FlightRecord still = registry.flight(myFlight.id());
        assertNotNull(still, "in-flight cargo is never deleted");
        assertEquals(FlightState.IN_FLIGHT, still.state());
        assertFalse(still.ownedBy(erased));

        RouteRegistry reloaded = roundTrip(registry);
        assertFalse(reloaded.retainsData(erased), "the erasure survives a reload");
    }
}
