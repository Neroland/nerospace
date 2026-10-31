package za.co.neroland.nerospace.world.planet.nyxara;

import za.co.neroland.nerospace.block.WeatherFlipBlock;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * Nyxara's flare effects: Dawnband Stone and the Nyx Prism flip to {@code FLARING} when a flare starts and back when
 * it ends (one capped scan per block type per transition). Shadow Lichen withering needs no hook (its random tick reads the weather). Call once at
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
        // The Nyx Prism has its own registration, so its own scan; the per-chunk cap is the shared Dawnband one.
        WeatherFlipBlock.register(WeatherEvent.FLARE, NyxaraContent.NYX_PRISM::get);
    }
}
