package za.co.neroland.nerospace.world.planet.greenxertz;

/**
 * Greenxertz's spore-bloom hook. The bloom's gameplay runs elsewhere: Spore Moss reads the event itself (spread x3 per
 * random tick, never extra ticks) and the exposed-player nausea is in {@code PlanetWeather}. The visible part (drifting
 * spores, pink during a bloom) is spawned on each client by {@code client.WeatherAmbience} within the player's own
 * particle budget, so the server sends no particles. {@link #particles} is kept as the reference bloom density.
 */
public final class GreenxertzWeather {

    /** Particles per player per second at full intensity (reference density). */
    public static final int MAX_PARTICLES = 24;

    private static boolean registered;

    private GreenxertzWeather() {
    }

    /** Call once at common init. Nothing server-side to register any more; kept so the init order stays stable. */
    public static synchronized void register() {
        registered = true;
    }

    /** Whether {@link #register} ran. */
    public static boolean registered() {
        return registered;
    }

    /** Pure: particles per player for an intensity in [0, 1]. */
    public static int particles(float intensity) {
        float i = Math.max(0.0F, Math.min(1.0F, intensity));
        return Math.round(MAX_PARTICLES * i);
    }
}
