package za.co.neroland.nerospace.world.planet;

/**
 * Pure rules for sleeping on a planet (no level, no server), so they are unit-testable. Vanilla time stays frozen on
 * every {@link ClockSource#PLANET_CLOCK} world; a bed there is gated on the planet clock's phase instead, and a full
 * night's sleep moves the planet clock to dawn. Phase convention as {@link ClockMath}: 0 sunrise, 0.5 sunset.
 *
 * <ul>
 *   <li>A clocked planet allows sleep while its phase is in the night window (default [0.52, 0.98): vanilla's
 *       12 542-23 459 of 24 000). Phase, not daylight, so an eclipse or a long dusk never opens it early.</li>
 *   <li>A frozen clock (ops freeze, gallery sky) still allows sleep in the window, but nothing is skipped: each
 *       sleeper rests and is woken alone. With the clock switched off every clock reads noon, so nobody sleeps.</li>
 *   <li>Nyxara has no night to wait for: a bed on the Nightside rests (no skip); the band and the Dayside refuse.</li>
 *   <li>The station never sleeps (its beds still set the respawn point).</li>
 *   <li>Greenxertz mirrors the vanilla day: vanilla owns its sleep and its skip.</li>
 * </ul>
 */
public final class PlanetSleepRules {

    /** The default night window, start inclusive, end exclusive. */
    public static final Window DEFAULT_WINDOW = new Window(0.52, 0.98);
    /** Ticks a player must lie in bed before a skip or a rest wake-up (vanilla's deep sleep). */
    public static final int DEEP_SLEEP_TICKS = 100;

    public static final String KEY_DAY = "nerospace.sleep.day";
    public static final String KEY_FROZEN = "nerospace.sleep.frozen";
    public static final String KEY_STATION = "nerospace.sleep.station";
    public static final String KEY_NYXARA_BRIGHT = "nerospace.sleep.nyxara_bright";

    private PlanetSleepRules() {
    }

    /** A phase window {@code [from, to)} in [0,1); a window with {@code from > to} wraps through sunrise. */
    public record Window(double from, double to) {

        public boolean contains(double phase) {
            double p = DaylightCurve.wrap(phase);
            return this.from <= this.to ? p >= this.from && p < this.to : p >= this.from || p < this.to;
        }
    }

    /** What kind of world the bed is on, for the sleep rules. */
    public enum World {
        /** Greenxertz: vanilla time, vanilla sleep. */
        VANILLA,
        /** A planet with a moving clock (Luna, Mars, Cindara, Glacira). */
        CLOCKED,
        /** Nyxara: a fixed clock, sleep by band position. */
        NYXARA,
        /** The orbital station: a fixed clock, never sleeps. */
        STATION
    }

    /** Nyxara band position of a bed. */
    public enum Side { DAYSIDE, BAND, NIGHTSIDE }

    /** The answer to "may this player get into bed now?". */
    public enum Decision {
        /** Not ours to decide (Greenxertz, or not a planet): vanilla rules apply. */
        VANILLA(null),
        /** Sleep; a full night's sleep moves the planet clock to dawn. */
        SLEEP_SKIP(null),
        /** Sleep, but nothing is skipped: the sleeper is woken alone after a deep sleep. */
        SLEEP_REST(null),
        /** Refused: outside the planet's night window (or the clock is switched off). */
        REFUSE_DAY(KEY_DAY),
        /** Refused: the station. */
        REFUSE_STATION(KEY_STATION),
        /** Refused: Nyxara's band or Dayside. */
        REFUSE_NYXARA_BRIGHT(KEY_NYXARA_BRIGHT);

        private final String messageKey;

        Decision(String messageKey) {
            this.messageKey = messageKey;
        }

        public boolean refused() {
            return this.messageKey != null;
        }

        /** The action-bar line for a refusal, or null when the player may sleep. */
        public String messageKey() {
            return this.messageKey;
        }
    }

    /**
     * Whether a player may get into bed.
     *
     * @param world        the kind of world
     * @param clockEnabled {@code clock.enabled}; off, every clock reads noon
     * @param frozen       the planet clock is frozen (ops freeze, gallery sky)
     * @param phase        the planet clock phase now
     * @param window       the planet's night window
     * @param side         Nyxara only: the bed's band position (null elsewhere)
     */
    public static Decision decide(World world, boolean clockEnabled, boolean frozen, double phase, Window window,
            Side side) {
        switch (world) {
            case VANILLA:
                return Decision.VANILLA;
            case STATION:
                return Decision.REFUSE_STATION;
            case NYXARA:
                return side == Side.NIGHTSIDE ? Decision.SLEEP_REST : Decision.REFUSE_NYXARA_BRIGHT;
            case CLOCKED:
            default:
                if (!clockEnabled || !window.contains(phase)) {
                    return Decision.REFUSE_DAY;
                }
                return frozen ? Decision.SLEEP_REST : Decision.SLEEP_SKIP;
        }
    }

    /** Vanilla's sleepers-needed maths: at least one, else the percentage of active (non-spectator) players. */
    public static int sleepersNeeded(int activePlayers, int percentage) {
        return Math.max(1, (int) Math.ceil(activePlayers * percentage / 100.0F));
    }

    /**
     * Whether enough players sleep, and sleep deeply enough, to skip the night (vanilla's rule, evaluated per level).
     * A percentage above 100 never skips (vanilla's "sleeping through this night is not possible").
     */
    public static boolean skips(int percentage, int activePlayers, int sleeping, int deepSleeping) {
        if (percentage > 100 || sleeping <= 0) {
            return false;
        }
        int needed = sleepersNeeded(activePlayers, percentage);
        return sleeping >= needed && deepSleeping >= needed;
    }
}
