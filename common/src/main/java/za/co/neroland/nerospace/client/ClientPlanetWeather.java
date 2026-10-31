package za.co.neroland.nerospace.client;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.network.PlanetWeatherPayload;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherState;

/**
 * Client-side copy of the current level's weather, filled by {@link PlanetWeatherPayload}. Pure data holder
 * (no client-only imports). Render-only — the server decides everything.
 *
 * <p>Smoothing (sky upgrade): the server steps intensity in jumps (and {@code weather clear} or the sky gallery drop
 * it to 0 in one packet), so {@link #clientTick} eases a client-side copy toward it with a 0.6 s time constant, and
 * remembers a {@link #recovery} after-image for a minute after an event clears. Everything weather-driven on the sky
 * and the ambient particles reads {@link #smoothedIntensity} / {@link #smoothedEvent}, so nothing pops. Two floats
 * and two ints of state; no allocation.</p>
 */
public final class ClientPlanetWeather {

    @Nullable
    private static volatile PlanetWeatherPayload current;

    /** Ticks for the recovery after-image to fade from 1 to 0 after an event clears. */
    public static final int RECOVERY_TICKS = 1200;
    /** Smoothing time constant in ticks (0.6 s). */
    static final float TAU_TICKS = 12.0F;

    private static float smoothed;
    private static float previousSmoothed;
    private static WeatherEvent lastEvent = WeatherEvent.NONE;
    private static WeatherState lastState = WeatherState.CLEAR;
    private static WeatherEvent recoveryEvent = WeatherEvent.NONE;
    private static int recoveryTicks;
    @Nullable
    private static Object lastDimension;

    private ClientPlanetWeather() {
    }

    public static void accept(PlanetWeatherPayload payload) {
        current = Float.isFinite(payload.intensity()) ? payload : null;
    }

    private static boolean matches(@Nullable PlanetWeatherPayload c, @Nullable Level level) {
        return c != null && level != null && c.dimension().equals(level.dimension().identifier());
    }

    public static WeatherEvent event(@Nullable Level level) {
        PlanetWeatherPayload c = current;
        if (!matches(c, level)) {
            return WeatherEvent.NONE;
        }
        WeatherEvent[] all = WeatherEvent.values();
        int i = c.event();
        return i >= 0 && i < all.length ? all[i] : WeatherEvent.NONE;
    }

    public static WeatherState state(@Nullable Level level) {
        PlanetWeatherPayload c = current;
        if (!matches(c, level)) {
            return WeatherState.CLEAR;
        }
        WeatherState[] all = WeatherState.values();
        int i = c.state();
        return i >= 0 && i < all.length ? all[i] : WeatherState.CLEAR;
    }

    public static float intensity(@Nullable Level level) {
        PlanetWeatherPayload c = current;
        return matches(c, level) ? Math.max(0.0F, Math.min(1.0F, c.intensity())) : 0.0F;
    }

    /**
     * Advances the smoothing one client tick (called once per tick by every loader, from
     * {@code WeatherAmbience.tick}). A change of level resets the state to the new level's synced value.
     */
    public static void clientTick(@Nullable Level level) {
        if (level == null) {
            reset(null, 0.0F);
            return;
        }
        Object dim = level.dimension();
        float target = intensity(level);
        if (!dim.equals(lastDimension)) {
            reset(dim, target);
        }
        WeatherState state = state(level);
        WeatherEvent event = event(level);
        step(target, state, event);
    }

    /** One smoothing step toward {@code target} (package-private for the unit test). */
    static void step(float target, WeatherState state, WeatherEvent event) {
        previousSmoothed = smoothed;
        smoothed += (target - smoothed) * Math.min(1.0F, 1.0F / TAU_TICKS);
        if (Math.abs(target - smoothed) < 1e-4F) {
            smoothed = target;
        }
        if (recoveryTicks > 0) {
            recoveryTicks--;
        }
        if (state == WeatherState.CLEAR && lastState != WeatherState.CLEAR && lastEvent != WeatherEvent.NONE) {
            recoveryEvent = lastEvent;
            recoveryTicks = RECOVERY_TICKS;
        }
        if (event != WeatherEvent.NONE) {
            lastEvent = event;
        }
        lastState = state;
    }

    static void reset(@Nullable Object dimension, float intensity) {
        lastDimension = dimension;
        smoothed = intensity;
        previousSmoothed = intensity;
        lastEvent = WeatherEvent.NONE;
        lastState = WeatherState.CLEAR;
        recoveryEvent = WeatherEvent.NONE;
        recoveryTicks = 0;
    }

    /** The eased intensity 0..1 (interpolated within the tick), or 0 when this is not the tracked level. */
    public static float smoothedIntensity(@Nullable Level level, float partialTick) {
        if (level == null || !level.dimension().equals(lastDimension)) {
            return 0.0F;
        }
        return smoothedAt(partialTick);
    }

    static float smoothedAt(float partialTick) {
        float t = Math.max(0.0F, Math.min(1.0F, partialTick));
        return Math.max(0.0F, Math.min(1.0F, previousSmoothed + (smoothed - previousSmoothed) * t));
    }

    /**
     * The event the smoothed intensity belongs to: the synced event, or while it is still fading out after the server
     * cleared it, the last active one.
     */
    public static WeatherEvent smoothedEvent(@Nullable Level level) {
        WeatherEvent now = event(level);
        if (now != WeatherEvent.NONE || level == null || !level.dimension().equals(lastDimension)) {
            return now;
        }
        return smoothed > 1e-3F ? lastEvent : WeatherEvent.NONE;
    }

    /** The event whose {@link #recovery} after-image is running ({@code NONE} when none). */
    public static WeatherEvent recoveryEvent(@Nullable Level level) {
        return level != null && level.dimension().equals(lastDimension) && recoveryTicks > 0 ? recoveryEvent
                : WeatherEvent.NONE;
    }

    /** 1 just after {@link #recoveryEvent} cleared, falling linearly to 0 over {@link #RECOVERY_TICKS}. */
    public static float recovery(@Nullable Level level) {
        return recoveryEvent(level) == WeatherEvent.NONE ? 0.0F : recoveryTicks / (float) RECOVERY_TICKS;
    }

    static float recoveryForTest() {
        return recoveryTicks / (float) RECOVERY_TICKS;
    }

    static WeatherEvent recoveryEventForTest() {
        return recoveryTicks > 0 ? recoveryEvent : WeatherEvent.NONE;
    }
}
