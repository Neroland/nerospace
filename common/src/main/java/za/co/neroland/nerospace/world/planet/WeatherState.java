package za.co.neroland.nerospace.world.planet;

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
        return CLEAR;
    }
}
