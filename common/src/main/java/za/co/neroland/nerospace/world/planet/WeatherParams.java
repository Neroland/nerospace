package za.co.neroland.nerospace.world.planet;

/**
 * Tuning for a planet's weather event: the mean interval between events and the duration
 * range of the PEAK phase, all in ticks. {@link #NONE} is a planet with no weather.
 */
public record WeatherParams(WeatherEvent event, long meanIntervalTicks, int minDurationTicks, int maxDurationTicks) {

    public static final WeatherParams NONE = new WeatherParams(WeatherEvent.NONE, 0L, 0, 0);

    public WeatherParams {
        event = event == null ? WeatherEvent.NONE : event;
        meanIntervalTicks = Math.max(0L, meanIntervalTicks);
        minDurationTicks = Math.max(0, minDurationTicks);
        maxDurationTicks = Math.max(minDurationTicks, maxDurationTicks);
    }

    public boolean hasWeather() {
        return this.event != WeatherEvent.NONE && this.meanIntervalTicks > 0L;
    }

    /** The same parameters scaled by the config multipliers (frequency shortens the interval). */
    public WeatherParams scaled(double frequencyMultiplier, double durationMultiplier) {
        if (!hasWeather()) {
            return this;
        }
        double f = Math.max(0.01, frequencyMultiplier);
        double d = Math.max(0.01, durationMultiplier);
        return new WeatherParams(this.event, Math.max(20L, Math.round(this.meanIntervalTicks / f)),
                Math.max(20, (int) Math.round(this.minDurationTicks * d)),
                Math.max(20, (int) Math.round(this.maxDurationTicks * d)));
    }
}
