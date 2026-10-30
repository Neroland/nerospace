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
        // First: binds Nyxara's stored band width before any chunk generation reads it.
        za.co.neroland.nerospace.world.planet.nyxara.worldgen.NyxaraBand.bind(server);
        PlanetClock.tick(server);
        PlanetWeather.tick(server);
        PlanetSync.tick(server);
        za.co.neroland.nerospace.block.BlockTicks.serverTick(server);
        za.co.neroland.nerospace.world.planet.greenxertz.BioBarkHeal.tick(server);
        // Village Cores that loaded since last tick: apply saved pending erasures, refresh the owner index.
        za.co.neroland.nerospace.village.VillageCoreBlockEntity.syncLedger(server);
    }
}
