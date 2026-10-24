package za.co.neroland.nerospace.world.planet;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Pushes the clock + weather snapshot to a player whenever they arrive in a level (join, respawn, any
 * dimension change), detected from the server tick so no extra loader event is needed. The memo is keyed by
 * the live player object in a weak map — no UUIDs, nothing persisted, gone when the player object is.
 */
public final class PlanetSync {

    private static final Map<ServerPlayer, ResourceKey<Level>> LAST = new WeakHashMap<>();

    private PlanetSync() {
    }

    public static synchronized void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ResourceKey<Level> dim = player.level().dimension();
            if (!dim.equals(LAST.get(player))) {
                LAST.put(player, dim);
                PlanetClock.syncTo(player);
                PlanetWeather.syncTo(player);
            }
        }
    }
}
