package za.co.neroland.nerospace.progression;

import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * Pure rules for the Worlds chapters' "survive the weather" step. A player survives a planet's weather by
 * staying out under the open sky, alive and on that planet, for {@link #SURVIVE_TICKS} while the planet's weather
 * event is running. Luna has no weather, so its trial is the lunar night (daylight at or below
 * {@link #LUNA_NIGHT_DAYLIGHT}).
 */
public final class WeatherSurvivalRules {

    /** Two minutes of continuous exposure. */
    public static final long SURVIVE_TICKS = 2_400L;
    /** Luna counts as "weather" while the daylight is at or below this (the lunar night). */
    public static final double LUNA_NIGHT_DAYLIGHT = 0.05;

    private WeatherSurvivalRules() {
    }

    /** Whether the exposure clock runs: under open sky, while the planet's trial is on. */
    public static boolean trialActive(WeatherEvent event, boolean lunaRules, double daylight, boolean underOpenSky) {
        if (!underOpenSky) {
            return false;
        }
        if (lunaRules) {
            return daylight <= LUNA_NIGHT_DAYLIGHT;
        }
        return event != null && event != WeatherEvent.NONE;
    }

    /** Whether an exposure that started at {@code since} has lasted long enough at {@code now}. */
    public static boolean survived(long since, long now) {
        return now - since >= SURVIVE_TICKS;
    }
}
