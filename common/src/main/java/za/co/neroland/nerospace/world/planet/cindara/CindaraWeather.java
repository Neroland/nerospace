package za.co.neroland.nerospace.world.planet.cindara;

import za.co.neroland.nerospace.block.LayerWeather;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/** Cindara ash-fall effects: Ash Layer accumulation and ember strikes. Call once at common init (idempotent). */
public final class CindaraWeather {

    private static boolean registered;

    private CindaraWeather() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        LayerWeather.register(WeatherEvent.ASH_FALL, CindaraContent.ASH_LAYER::get);
        EmberStrike.register();
    }
}
