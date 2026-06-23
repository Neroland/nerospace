package za.co.neroland.nerospace.world;

import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerospace.network.OxygenFieldSyncPayload;
import za.co.neroland.nerospace.registry.ModDimensions;

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

    /** Dimensions whose atmosphere is driven by the oxygen field. */
    public static final Set<ResourceKey<Level>> FIELD_DIMENSIONS = Set.of(
            ModDimensions.GREENXERTZ_LEVEL, ModDimensions.CINDARA_LEVEL, ModDimensions.STATION_LEVEL,
            ModDimensions.GLACIRA_LEVEL);

    /** Server ticks between field relaxation passes (inlined from Config.OXYGEN_SIM_INTERVAL_TICKS). */
    private static final int SIM_INTERVAL_TICKS = 5;
    /** How often the range-limited field view is pushed to nearby clients (for the visual layers). */
    private static final int SYNC_INTERVAL_TICKS = 10;
    /** Blocks around a player the field snapshot covers (inlined from Config.OXYGEN_SYNC_RADIUS). */
    private static final int SYNC_RADIUS = 32;

    private OxygenFieldEvents() {
    }

    /** Runs one throttled field pass per eligible dimension, and syncs the nearby field to clients. */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!FIELD_DIMENSIONS.contains(level.dimension())) {
                continue;
            }
            long time = level.getGameTime();
            OxygenFieldManager manager = OxygenFieldManager.get(level);
            if (time % SIM_INTERVAL_TICKS == 0) {
                manager.simulate(level);
            }
            if (time % SYNC_INTERVAL_TICKS == 0) {
                for (ServerPlayer player : level.players()) {
                    ModNetwork.sendToPlayer(player,
                            OxygenFieldSyncPayload.of(manager.snapshotAround(player.blockPosition(), SYNC_RADIUS)));
                }
            }
        }
    }
}
