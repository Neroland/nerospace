package za.co.neroland.nerospace.world;

import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.network.OxygenFieldSyncPayload;
import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Server-side driver for the oxygen field (terraform design §1.3). Runs the throttled relaxation pass
 * on each airless Nerospace dimension. Cheap when idle: the manager pauses simulation for sources with
 * no nearby player and short-circuits when there are no sources and no live cells.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class OxygenFieldEvents {

    /** Dimensions whose atmosphere is driven by the oxygen field (mirrors GreenxertzAtmosphere). */
    public static final Set<ResourceKey<Level>> FIELD_DIMENSIONS = Set.of(
            ModDimensions.GREENXERTZ_LEVEL, ModDimensions.CINDARA_LEVEL, ModDimensions.STATION_LEVEL,
            ModDimensions.GLACIRA_LEVEL);

    /** How often the range-limited field view is pushed to nearby clients. */
    private static final int SYNC_INTERVAL_TICKS = 10;

    private OxygenFieldEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !FIELD_DIMENSIONS.contains(level.dimension())) {
            return;
        }
        long time = level.getGameTime();
        OxygenFieldManager manager = OxygenFieldManager.get(level);

        if (time % Config.OXYGEN_SIM_INTERVAL_TICKS.get() == 0) {
            manager.simulate(level);
        }

        if (time % SYNC_INTERVAL_TICKS == 0) {
            int radius = Config.OXYGEN_SYNC_RADIUS.get();
            for (ServerPlayer player : level.players()) {
                OxygenFieldSyncPayload payload =
                        OxygenFieldSyncPayload.of(manager.snapshotAround(player.blockPosition(), radius));
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }
}
