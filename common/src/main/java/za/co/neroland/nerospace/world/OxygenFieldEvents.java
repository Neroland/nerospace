package za.co.neroland.nerospace.world;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerospace.network.OxygenFieldSyncPayload;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * Cross-loader server-side driver for the oxygen field (terraform design §1.3). Each loader calls
 * {@link #tick(MinecraftServer)} once per server tick from its own hook (alongside the meteor and
 * oxygen-survival drivers); this runs the throttled relaxation pass on each airless Nerospace
 * dimension. Cheap when idle: the manager pauses simulation for sources with no nearby player and
 * short-circuits when there are no sources and no live cells.
 *
 * <p>Cross-loader port note: the client field sync (range-limited snapshot → {@code ClientOxygenField}
 * for the particle / haze / boundary visual layers) is the deferred follow-up; this batch is the
 * server field + breathability only. The sim interval is inlined (config seam deferred).</p>
 */
public final class OxygenFieldEvents {

    /** Whether a dimension's atmosphere is driven by the oxygen field: every airless Nerospace planet. */
    public static boolean isFieldDimension(ResourceKey<Level> dimension) {
        return Planets.isPlanet(dimension) && OxygenManager.isAirless(dimension);
    }

    /** Server ticks between field relaxation passes (inlined from Config.OXYGEN_SIM_INTERVAL_TICKS). */
    private static final int SIM_INTERVAL_TICKS = 5;
    /** How often the range-limited field view is pushed to nearby clients (for the visual layers). */
    private static final int SYNC_INTERVAL_TICKS = 10;
    /** Blocks around a player the field snapshot covers (inlined from Config.OXYGEN_SYNC_RADIUS). */
    private static final int SYNC_RADIUS = 32;

    /** Last snapshot each player was sent. Weak keys: a logged-out player is dropped with no erasure work. */
    private static final Map<ServerPlayer, SyncState> SENT = Collections.synchronizedMap(new WeakHashMap<>());

    private OxygenFieldEvents() {
    }

    /** Per-player memory of the view already on the client. Dimension is part of identity: one player object changes levels. */
    private static final class SyncState {
        boolean known;
        int revision;
        long pos;
        long hash;
        ResourceKey<Level> dimension;
    }

    /** Runs one throttled field pass per eligible dimension, and syncs the nearby field to clients. */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!isFieldDimension(level.dimension())) {
                continue;
            }
            long time = level.getGameTime();
            OxygenFieldManager manager = OxygenFieldManager.get(level);
            if (time % SIM_INTERVAL_TICKS == 0) {
                manager.simulate(level);
            }
            if (time % SYNC_INTERVAL_TICKS == 0) {
                int revision = manager.revision();
                for (ServerPlayer player : level.players()) {
                    syncPlayer(level, manager, player, revision);
                }
            }
        }
    }

    /**
     * Sends the player's radius-32 view only when it can differ from what they already have: the field revision
     * moved, they moved, or this is their first view in this dimension. A move through an unchanged region still
     * builds the snapshot (the window slid) but skips the packet when the cells match.
     */
    private static void syncPlayer(ServerLevel level, OxygenFieldManager manager, ServerPlayer player, int revision) {
        long pos = player.blockPosition().asLong();
        SyncState state = SENT.computeIfAbsent(player, p -> new SyncState());
        boolean known = state.known && level.dimension().equals(state.dimension);
        if (OxygenSyncRules.skipBuild(known, state.revision, state.pos, revision, pos)) {
            return;
        }
        Long2ByteMap snap = manager.snapshotAround(player.blockPosition(), SYNC_RADIUS);
        long hash = OxygenSyncRules.fingerprint(snap);
        state.revision = revision;
        state.pos = pos;
        state.dimension = level.dimension();
        state.known = true;
        if (OxygenSyncRules.skipSend(known, state.hash, hash)) {
            state.hash = hash;
            return;
        }
        state.hash = hash;
        ModNetwork.sendToPlayer(player, OxygenFieldSyncPayload.of(snap));
    }
}
