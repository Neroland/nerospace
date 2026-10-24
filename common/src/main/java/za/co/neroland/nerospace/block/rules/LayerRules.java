package za.co.neroland.nerospace.block.rules;

/**
 * Snow-like layer accumulation (002 §1.3 Ash Layer, §3): weather adds one layer per chunk per
 * {@link #INTERVAL_TICKS}, capped at {@link #WEATHER_CAP}; players stack to {@link #MAX}. Walkers slow at 3+.
 */
public final class LayerRules {

    public static final int MAX = 8;
    public static final int WEATHER_CAP = 4;
    public static final int INTERVAL_TICKS = 40;
    public static final int SLOW_FROM = 3;
    public static final float SLOW = 0.7F;

    private LayerRules() {
    }

    /** The layer count after one addition, or {@code current} when the relevant cap is reached. 0 = absent. */
    public static int nextLayers(int current, int weatherCap, boolean fromWeather) {
        int cap = fromWeather ? Caps.lower(WEATHER_CAP, weatherCap) : MAX;
        if (current >= cap) {
            return current;
        }
        return Math.max(1, current + 1);
    }

    public static float slowFactor(int layers) {
        return layers >= SLOW_FROM ? SLOW : 1.0F;
    }

    /** Melting: −1 layer per random tick only next to fire or lava; 0 means remove. */
    public static int melt(int current, boolean fireOrLavaAdjacent) {
        return fireOrLavaAdjacent ? Math.max(0, current - 1) : current;
    }
}
