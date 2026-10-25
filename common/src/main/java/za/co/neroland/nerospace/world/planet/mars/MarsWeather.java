package za.co.neroland.nerospace.world.planet.mars;

import za.co.neroland.nerospace.block.DriftWeather;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.mars.entity.DustDevilWeather;

/** Mars dust-storm effects: Red Regolith dune drift and Dust Devil Pit spawning. Call once at common init. */
public final class MarsWeather {

    private static boolean registered;

    private MarsWeather() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        DriftWeather.register(WeatherEvent.DUST_STORM, MarsContent.RED_REGOLITH::get);
        DustDevilWeather.register();
    }
}
