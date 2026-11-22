package za.co.neroland.nerospace.world.planet;

import za.co.neroland.nerospace.data.LenientDecode;

/** Phase of the weather state machine. Persisted <b>by name</b>; unknown names load as {@link #CLEAR}. */
public enum WeatherState {
    CLEAR,
    RISING,
    PEAK,
    FADING;

    public static WeatherState byName(String name) {
        if (name != null) {
            for (WeatherState s : values()) {
                if (s.name().equals(name)) {
                    return s;
                }
            }
        }
        if (LenientDecode.unknownSaved(name, false)) {
            LenientDecode.warnOnce("weather state");
        }
        return CLEAR;
    }
}
