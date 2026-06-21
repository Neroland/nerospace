package za.co.neroland.nerospace.world;

import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

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

    private OxygenFieldEvents() {
    }

    /** Runs one throttled field pass per eligible dimension. */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!FIELD_DIMENSIONS.contains(level.dimension())) {
                continue;
            }
            if (level.getGameTime() % SIM_INTERVAL_TICKS == 0) {
                OxygenFieldManager.get(level).simulate(level);
            }
        }
    }
}
