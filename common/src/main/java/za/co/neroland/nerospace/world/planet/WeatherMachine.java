package za.co.neroland.nerospace.world.planet;

import java.util.function.DoubleSupplier;

/**
 * The pure weather state machine, stepped once per second:
 * {@code CLEAR --(roll after cooldown)--> RISING --(reach target)--> PEAK --(duration)--> FADING --> CLEAR}.
 * Intensity ramps {@link #RAMP_PER_SECOND} per second; the target is rolled in [0.4, 1.0].
 *
 * <p>Mutable value object; {@link PlanetWeatherState} persists its fields by name. No level access — the
 * caller passes the random source and whether conditions currently allow an event to start.</p>
 */
public final class WeatherMachine {

    public static final int STEP_TICKS = 20;
    public static final float RAMP_PER_SECOND = 0.02F;
    public static final float MIN_TARGET = 0.4F;
    public static final float MAX_TARGET = 1.0F;

    public WeatherState state = WeatherState.CLEAR;
    public WeatherEvent event = WeatherEvent.NONE;
    public float intensity;
    public float target;
    public int remainingTicks;
    public int cooldownTicks = -1;
    public long seed;

    /** What changed in one step, so the caller knows when to sync and fire start/end hooks. */
    public enum Change {
        NONE, STARTED, STATE, ENDED
    }

    /**
     * Advances one second.
     *
     * @param params      the planet's (config-scaled) weather parameters
     * @param enabled     false forces CLEAR immediately (config switched off)
     * @param mayStart    whether conditions allow a new event to begin right now (eclipse, darkness …)
     * @param random      uniform [0,1) source
     */
    public Change step(WeatherParams params, boolean enabled, boolean mayStart, DoubleSupplier random) {
        if (!enabled || !params.hasWeather()) {
            if (this.state != WeatherState.CLEAR || this.intensity != 0.0F) {
                forceClear(params);
                return Change.ENDED;
            }
            return Change.NONE;
        }
        long halfInterval = Math.max(STEP_TICKS, params.meanIntervalTicks() / 2L);
        if (this.cooldownTicks < 0) {
            this.cooldownTicks = (int) Math.min(Integer.MAX_VALUE, halfInterval);
        }
        switch (this.state) {
            case CLEAR -> {
                if (this.cooldownTicks > 0) {
                    this.cooldownTicks = Math.max(0, this.cooldownTicks - STEP_TICKS);
                    return Change.NONE;
                }
                double chance = (double) STEP_TICKS / (double) halfInterval;
                if (!mayStart || random.getAsDouble() >= chance) {
                    return Change.NONE;
                }
                this.event = params.event();
                this.state = WeatherState.RISING;
                this.intensity = 0.0F;
                this.target = (float) (MIN_TARGET + (MAX_TARGET - MIN_TARGET) * random.getAsDouble());
                int span = params.maxDurationTicks() - params.minDurationTicks();
                this.remainingTicks = params.minDurationTicks() + (int) Math.round(span * random.getAsDouble());
                this.seed = (long) (random.getAsDouble() * Long.MAX_VALUE);
                return Change.STARTED;
            }
            case RISING -> {
                this.intensity = Math.min(this.target, this.intensity + RAMP_PER_SECOND);
                if (this.intensity >= this.target) {
                    this.state = WeatherState.PEAK;
                    return Change.STATE;
                }
                return Change.NONE;
            }
            case PEAK -> {
                this.remainingTicks = Math.max(0, this.remainingTicks - STEP_TICKS);
                if (this.remainingTicks == 0) {
                    this.state = WeatherState.FADING;
                    return Change.STATE;
                }
                return Change.NONE;
            }
            case FADING -> {
                this.intensity = Math.max(0.0F, this.intensity - RAMP_PER_SECOND);
                if (this.intensity <= 0.0F) {
                    forceClear(params);
                    return Change.ENDED;
                }
                return Change.NONE;
            }
            default -> {
                return Change.NONE;
            }
        }
    }

    /** Ends any event now and restarts the cooldown. */
    public void forceClear(WeatherParams params) {
        this.state = WeatherState.CLEAR;
        this.intensity = 0.0F;
        this.target = 0.0F;
        this.remainingTicks = 0;
        this.cooldownTicks = (int) Math.min(Integer.MAX_VALUE, Math.max(STEP_TICKS, params.meanIntervalTicks() / 2L));
        this.event = WeatherEvent.NONE;
    }

    /** Starts the planet's event now (ops command), skipping the cooldown and the start roll. */
    public void forceStart(WeatherParams params, float targetIntensity, int durationTicks, long newSeed) {
        if (!params.hasWeather()) {
            return;
        }
        this.event = params.event();
        this.state = WeatherState.RISING;
        this.intensity = 0.0F;
        this.target = Math.max(MIN_TARGET, Math.min(MAX_TARGET, targetIntensity));
        this.remainingTicks = Math.max(STEP_TICKS, durationTicks);
        this.seed = newSeed;
    }

    /**
     * Jumps a just-started event straight to its peak (the gallery's screenshot states): the intensity reaches its
     * target and the event holds there for its duration, then fades as usual. No-op when nothing is active.
     */
    public void reachPeak() {
        if (!active()) {
            return;
        }
        this.intensity = this.target;
        this.state = WeatherState.PEAK;
    }

    /** Whether an event is in progress (any state but CLEAR). */
    public boolean active() {
        return this.state != WeatherState.CLEAR && this.event != WeatherEvent.NONE;
    }
}
