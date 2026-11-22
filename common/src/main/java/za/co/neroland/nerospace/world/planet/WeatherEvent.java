package za.co.neroland.nerospace.world.planet;

import za.co.neroland.nerospace.data.LenientDecode;

/**
 * The one weather event a planet can have. Persisted <b>by name</b> (never by ordinal); unknown names load as
 * {@link #NONE}.
 */
public enum WeatherEvent {
    NONE,
    SPORE_BLOOM,
    DUST_STORM,
    ASH_FALL,
    DIAMOND_DUST,
    FLARE;

    public static WeatherEvent byName(String name) {
        if (name != null) {
            for (WeatherEvent e : values()) {
                if (e.name().equals(name)) {
                    return e;
                }
            }
        }
        if (LenientDecode.unknownSaved(name, false)) {
            LenientDecode.warnOnce("weather event");
        }
        return NONE;
    }
}
