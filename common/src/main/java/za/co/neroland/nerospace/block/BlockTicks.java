package za.co.neroland.nerospace.block;

import net.minecraft.server.MinecraftServer;

/**
 * Per-server-tick work for the 2.0 block behaviours that follow entities briefly (Hematite drop magnet, Frozen Geyser
 * fall grace, Spore Pod clouds) and queued weather transition scans. Every queue is empty almost always, so this
 * costs a few size checks per tick.
 * Called from {@code PlanetTicks.serverTick}, the common tick entry point every loader already drives.
 */
public final class BlockTicks {

    private BlockTicks() {
    }

    public static void serverTick(MinecraftServer server) {
        MagneticOreBlock.tickMagnets();
        GeyserBlock.tickGrace();
        SporeBurstBlock.tickBursts();
        WeatherScan.tick(server);
    }
}
