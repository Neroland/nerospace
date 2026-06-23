package za.co.neroland.nerospace.meteor;

import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.network.MeteorSyncPayload;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Server-side driver for meteor events (meteor-events design §3/§6): ticks the per-level
 * {@link MeteorEventManager} on eligible surface dimensions and pushes the nearest-site snapshot to
 * any player holding a Meteor Tracker. Cheap when idle — the manager short-circuits with no players.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class MeteorEvents {

    /** Surface worlds meteors fall on (the void station is excluded — nothing to crater). */
    public static final Set<ResourceKey<Level>> METEOR_DIMENSIONS = Set.of(
            Level.OVERWORLD,
            ModDimensions.GREENXERTZ_LEVEL,
            ModDimensions.CINDARA_LEVEL,
            ModDimensions.GLACIRA_LEVEL);

    /** How often the tracker snapshot is pushed to holders. */
    private static final int SYNC_INTERVAL_TICKS = 10;

    private MeteorEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !METEOR_DIMENSIONS.contains(level.dimension())) {
            return;
        }
        MeteorEventManager manager = MeteorEventManager.get(level);
        manager.tick(level);

        if (level.getGameTime() % SYNC_INTERVAL_TICKS == 0) {
            for (ServerPlayer player : level.players()) {
                if (!holdsTracker(player)) {
                    continue;
                }
                MeteorSite nearest = manager.nearestSite(player.blockPosition());
                PacketDistributor.sendToPlayer(player, nearest == null
                        ? MeteorSyncPayload.ABSENT
                        : new MeteorSyncPayload(true, nearest.pos, nearest.state));
            }
        }
    }

    private static boolean holdsTracker(ServerPlayer player) {
        return isTracker(player.getMainHandItem()) || isTracker(player.getOffhandItem());
    }

    private static boolean isTracker(ItemStack stack) {
        return stack.is(ModItems.METEOR_TRACKER.get());
    }
}
