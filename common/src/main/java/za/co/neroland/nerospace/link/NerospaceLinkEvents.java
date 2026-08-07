package za.co.neroland.nerospace.link;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.link.LinkAlert;
import za.co.neroland.nerolandcore.link.LinkAlerts;
import za.co.neroland.nerolandcore.link.LinkEvent;
import za.co.neroland.nerolandcore.link.NeroLinkRegistry;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.NerospacePlanets;
import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.RocketTier;
import za.co.neroland.nerospace.world.OxygenManager;

/**
 * Nerospace's live half of the NeroLink module: the state changes worth pushing to a companion client
 * rather than waiting for it to poll.
 *
 * <p>Scoping follows the ecosystem rule. {@link NerospaceLinkModule#TOPIC_OXYGEN_LOW} and
 * {@link NerospaceLinkModule#TOPIC_ROCKET_LANDED} are the player's own state and go only to that player
 * ({@link LinkEvent#forPlayer}). {@link NerospaceLinkModule#TOPIC_STATION_STATE} is a broadcast, so it
 * carries the absolute minimum that still means something — a slot number, a dimension and a state. Not
 * the station's name, not its owner, not its position.</p>
 *
 * <p><b>POPIA/GDPR.</b> The only player data held here is the in-memory edge/rate-limit bookkeeping keyed
 * by UUID, which exists purely so an event and its alert cannot flap. It is cleared by
 * {@link #forgetPlayer(UUID)}, wired into Nerospace's Core erasure hook, and it never reaches disk.
 * Warnings log a topic, never who the event was for.</p>
 */
public final class NerospaceLinkEvents {

    /** Fire {@code oxygen_low} once when the tank drops below this fraction of its capacity. */
    private static final int LOW_PERCENT = 25;
    /** Re-arm the edge once the tank recovers above this fraction, so a tank hovering at the line cannot flap. */
    private static final int REARM_PERCENT = 40;
    /** Minimum gap between two persisted low-oxygen alerts for the same player. */
    private static final long ALERT_COOLDOWN_MS = 300_000L;

    /** Players currently below the low-oxygen line (the edge latch). */
    private static final Set<UUID> OXYGEN_LOW = ConcurrentHashMap.newKeySet();
    /** Last time a low-oxygen alert was raised per player. */
    private static final Map<UUID, Long> LAST_ALERT = new ConcurrentHashMap<>();

    private NerospaceLinkEvents() {
    }

    /**
     * Intentionally empty. Nerospace publishes from the gameplay code paths themselves (the oxygen tick,
     * the rocket arrival, the station registry), so there is nothing to subscribe. This exists so the
     * module has the same three-surface shape as every other Nero mod and has one obvious place to grow.
     */
    static void init() {
    }

    /** POPIA/GDPR: drops a player's in-memory edge/rate-limit bookkeeping. Called from the erasure hook. */
    public static void forgetPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        OXYGEN_LOW.remove(playerId);
        LAST_ALERT.remove(playerId);
    }

    // --- oxygen_low (owner-scoped) -----------------------------------------

    /**
     * Called from the oxygen tick with the player's current tank reading. Fires once on the way DOWN past
     * {@link #LOW_PERCENT} and re-arms on the way back up past {@link #REARM_PERCENT}; a steady tank
     * therefore produces exactly one event per trip into the red, not one per tick.
     *
     * @param oxygen the player's current oxygen, already clamped to their tank size
     * @param max    the player's tank capacity
     */
    public static void oxygenChanged(ServerPlayer player, int oxygen, int max) {
        if (player == null || max <= 0 || !enabled()) {
            return;
        }
        UUID playerId = player.getUUID();
        int percent = Math.min(100, Math.max(0, oxygen * 100 / max));

        if (percent >= REARM_PERCENT) {
            OXYGEN_LOW.remove(playerId);
            return;
        }
        if (percent >= LOW_PERCENT || !OXYGEN_LOW.add(playerId)) {
            return;   // still above the line, or already reported for this trip into the red
        }

        try {
            ResourceKey<Level> dimension = player.level().dimension();
            JsonObject payload = new JsonObject();
            payload.addProperty("schema_version", NerospaceLinkModule.SCHEMA_VERSION);
            payload.addProperty("oxygen", oxygen);
            payload.addProperty("oxygen_max", max);
            payload.addProperty("oxygen_percent", percent);
            payload.addProperty("dimension", dimension.identifier().toString());
            payload.addProperty("airless", OxygenManager.isAirless(dimension));
            payload.addProperty("suit_tier", OxygenManager.suitTier(player).name());
            payload.addProperty("timestamp", System.currentTimeMillis());
            publish(LinkEvent.forPlayer(NerospaceLinkModule.MODULE_ID,
                    NerospaceLinkModule.TOPIC_OXYGEN_LOW, playerId, payload));
        } catch (RuntimeException e) {
            warn(NerospaceLinkModule.TOPIC_OXYGEN_LOW, e);
        }

        raiseOxygenAlert(player.level().getServer(), playerId);
    }

    /**
     * A persisted alert survives until acknowledged, so it must not flap: one per player per
     * {@link #ALERT_COOLDOWN_MS}, under a single stable id so a re-raise replaces rather than stacks.
     */
    private static void raiseOxygenAlert(@Nullable MinecraftServer server, UUID playerId) {
        if (server == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_ALERT.get(playerId);
        if (last != null && now - last < ALERT_COOLDOWN_MS) {
            return;
        }
        LAST_ALERT.put(playerId, now);
        try {
            LinkAlerts.get(server).raise(server, playerId,
                    LinkAlert.raise(NerospaceLinkModule.TOPIC_OXYGEN_LOW, NerospaceLinkModule.MODULE_ID,
                            LinkAlert.Severity.WARN, "Your oxygen is running low."));
        } catch (RuntimeException e) {
            warn("alerts", e);
        }
    }

    // --- rocket_landed (owner-scoped) --------------------------------------

    /** Called once a completed launch has set the rider down at their destination. */
    public static void rocketLanded(ServerPlayer player, ResourceKey<Level> destination, RocketTier tier,
            int fuelRemaining, int oxygenReserve, BlockPos landedAt) {
        if (player == null || destination == null || !enabled()) {
            return;
        }
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("schema_version", NerospaceLinkModule.SCHEMA_VERSION);
            payload.addProperty("dimension", destination.identifier().toString());
            NerospacePlanets.byDimension(destination)
                    .map(PlanetId::asString)
                    .ifPresent(planet -> payload.addProperty("planet", planet));
            if (tier != null) {
                payload.addProperty("tier", tier.name());
                payload.addProperty("tier_level", tier.level());
            }
            payload.addProperty("fuel_remaining", fuelRemaining);
            payload.addProperty("oxygen_reserve", oxygenReserve);
            if (landedAt != null) {
                JsonObject at = new JsonObject();
                at.addProperty("x", landedAt.getX());
                at.addProperty("y", landedAt.getY());
                at.addProperty("z", landedAt.getZ());
                payload.add("position", at);   // the requester's own arrival point
            }
            payload.addProperty("timestamp", System.currentTimeMillis());
            publish(LinkEvent.forPlayer(NerospaceLinkModule.MODULE_ID,
                    NerospaceLinkModule.TOPIC_ROCKET_LANDED, player.getUUID(), payload));
        } catch (RuntimeException e) {
            warn(NerospaceLinkModule.TOPIC_ROCKET_LANDED, e);
        }
    }

    // --- station_state (BROADCAST) -----------------------------------------

    /**
     * A station slot changed state ({@code FOUNDED} / {@code RENAMED}). This is the one broadcast in the
     * module, so the payload is deliberately anaemic: a slot number, the station dimension and a state
     * word. No name (the owner chose it), no owner UUID, no position — a listener learns that the orbital
     * registry moved, and nothing about whose it is.
     */
    public static void stationStateChanged(@Nullable MinecraftServer server, int slot, String state) {
        if (server == null || state == null || !enabled()) {
            return;
        }
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("schema_version", NerospaceLinkModule.SCHEMA_VERSION);
            payload.addProperty("station", slot);
            payload.addProperty("dimension", ModDimensions.STATION_LEVEL.identifier().toString());
            payload.addProperty("state", state);
            payload.addProperty("timestamp", System.currentTimeMillis());
            publish(LinkEvent.broadcast(NerospaceLinkModule.MODULE_ID,
                    NerospaceLinkModule.TOPIC_STATION_STATE, payload));
        } catch (RuntimeException e) {
            warn(NerospaceLinkModule.TOPIC_STATION_STATE, e);
        }
    }

    // --- plumbing ----------------------------------------------------------

    private static boolean enabled() {
        return NerospaceLinkAccess.enabled();
    }

    /** Publish to Core's shared bus; a failure there is logged, never thrown at the gameplay caller. */
    private static void publish(LinkEvent event) {
        try {
            NeroLinkRegistry.eventBus().publish(event);
        } catch (RuntimeException e) {
            warn(event.topic(), e);
        }
    }

    /** Topic only — never who the event was for (POPIA/GDPR). */
    private static void warn(String topic, RuntimeException e) {
        NerospaceCommon.LOGGER.warn("[Nerospace] Publishing the NeroLink '{}' event failed.", topic, e);
    }
}
