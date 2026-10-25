package za.co.neroland.nerospace.world.planet.glacira;

import za.co.neroland.nerospace.block.CrystalStageBlock;
import za.co.neroland.nerospace.world.planet.WeatherEffects;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * Glacira's diamond-dust effects: open water freezes while the event runs, and Frost Crystal Clusters grow one stage
 * when it ends. Call once at common init, after the registries.
 */
public final class GlaciraWeather {

    private static boolean registered;

    private GlaciraWeather() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        WeatherEffects.Registry.register(WeatherEvent.DIAMOND_DUST, new DiamondDustFreeze());
        CrystalStageBlock.register(WeatherEvent.DIAMOND_DUST, GlaciraContent.FROST_CRYSTAL_CLUSTER::get);
    }
}
