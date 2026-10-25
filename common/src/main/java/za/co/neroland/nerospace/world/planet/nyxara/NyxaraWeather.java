package za.co.neroland.nerospace.world.planet.nyxara;

import za.co.neroland.nerospace.block.WeatherFlipBlock;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * Nyxara's flare effects: Dawnband Stone flips to {@code FLARING} when a flare starts and back when it ends (one
 * capped scan per transition). Shadow Lichen withering needs no hook (its random tick reads the weather). Call once at
 * common init, after the registries.
 */
public final class NyxaraWeather {

    private static boolean registered;

    private NyxaraWeather() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        WeatherFlipBlock.register(WeatherEvent.FLARE, NyxaraContent.DAWNBAND_STONE::get);
    }
}
