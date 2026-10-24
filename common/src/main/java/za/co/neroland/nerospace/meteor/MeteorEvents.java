package za.co.neroland.nerospace.meteor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.network.MeteorSyncPayload;
import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Cross-loader server-side driver for natural meteor events (meteor-events design §3/§6). Each loader
 * calls {@link #tick(MinecraftServer)} once per server tick from its own hook (NeoForge
 * {@code ServerTickEvent.Post}, Fabric {@code ServerTickEvents.END_SERVER_TICK}); this ticks the
 * per-level {@link MeteorEventManager} on the eligible surface dimensions. Cheap when idle — the
 * manager short-circuits with no players online.
 *
 * <p>Cross-loader port note: the tracker push (nearest-site sync to Meteor Tracker holders) is a
 * deferred follow-up — it needs the Meteor Tracker item + a clientbound sync payload + a client HUD.
 * This batch is the server-side scheduler only; the {@link MeteorEventManager#nearestSite} hook is
 * already in place for it.</p>
 */
public final class MeteorEvents {

    /** Surface worlds meteors fall on: the Overworld and every planet with a surface (not the station void). */
    public static boolean isMeteorDimension(ResourceKey<Level> dimension) {
        if (Level.OVERWORLD.equals(dimension)) {
            return true;
        }
        PlanetDefinition planet = Planets.get(dimension);
        return planet != null && planet.meteorSurface();
    }

    /** How often the nearest-site snapshot is pushed to tracker holders. */
    private static final int SYNC_INTERVAL_TICKS = 10;

    private MeteorEvents() {
    }

    /** Ticks the meteor scheduler on every eligible loaded dimension, and syncs the tracker readout. */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!isMeteorDimension(level.dimension())) {
                continue;
            }
            MeteorEventManager manager = MeteorEventManager.get(level);
            manager.tick(level);

            if (level.getGameTime() % SYNC_INTERVAL_TICKS == 0) {
                for (ServerPlayer player : level.players()) {
                    if (!holdsTracker(player)) {
                        continue;
                    }
                    MeteorSite nearest = manager.nearestSite(player.blockPosition());
                    ModNetwork.sendToPlayer(player, nearest == null
                            ? MeteorSyncPayload.ABSENT
                            : new MeteorSyncPayload(true, nearest.pos, nearest.state));
                }
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
