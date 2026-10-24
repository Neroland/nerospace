package za.co.neroland.nerospace.world.planet;

import net.minecraft.server.MinecraftServer;

/**
 * The single common entry point for the per-server-tick planet work (clock start-up, weather state machine,
 * client sync). Each loader calls {@link #serverTick} once per server tick from its own hook:
 * Fabric {@code ServerTickEvents.END_SERVER_TICK}, NeoForge {@code ServerTickEvent.Post},
 * Forge {@code TickEvent.ServerTickEvent.Post} (listed in {@code docs/MULTILOADER.md}).
 */
public final class PlanetTicks {

    private PlanetTicks() {
    }

    public static void serverTick(MinecraftServer server) {
        PlanetClock.tick(server);
        PlanetWeather.tick(server);
        PlanetSync.tick(server);
        za.co.neroland.nerospace.block.BlockTicks.serverTick(server);
    }
}
