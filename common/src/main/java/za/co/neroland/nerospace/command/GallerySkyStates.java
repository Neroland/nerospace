package za.co.neroland.nerospace.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import za.co.neroland.nerospace.world.planet.nyxara.worldgen.FixedXBands;

/**
 * The {@code /nerospace gallery sky} state table: for each world (by its lang id) the named sky states an operator can
 * show, and what each one does. Pure data and arithmetic (no level, no server), so the table and the parsing are
 * unit-tested.
 *
 * <p>A state sets the planet clock phase (then freezes it), starts or clears the planet's weather, and for
 * {@code here} says where to stand and which way to look. Every set piece of the planet skies is driven by the clock
 * phase, the weather and the viewing position, so a state's phase / weather / view is what shows it: Earthrise (Earth
 * with its night side and city lights rising in the east), the blue sunset (the blue aureole of a sun on the horizon),
 * the eclipse corona and diamond ring, the aurora curtains and the diamond-dust rose fringe, the spore shimmer, the
 * flare ray fans, the band's refraction bow, the station's sunrise through the homeworld's limb. The facing looks at
 * it, and the {@link State#sky()} id names the look ({@code GallerySkyStatesTest} checks each one shows).</p>
 */
public final class GallerySkyStates {

    /** Phases on the shared convention: 0 sunrise, 0.25 noon, 0.5 sunset, 0.75 midnight. */
    public static final double DAWN = 0.02;
    public static final double NOON = 0.25;
    public static final double DUSK = 0.48;
    public static final double NIGHT = 0.75;
    /** "Leave the clock alone" (fixed-clock worlds whose sky depends on where you stand). */
    public static final double KEEP_PHASE = Double.NaN;
    /** "Keep the player's facing". */
    public static final float KEEP = Float.NaN;

    /** Blocks kept between a Nyxara side viewing spot and the band's ragged edge. */
    static final int SIDE_CLEARANCE = 96;

    /** What a state does to the planet's weather. */
    public enum Weather {
        /** Ends any event, so the sky is clear. */
        CLEAR,
        /** Starts the planet's own event at full strength. */
        START
    }

    /** Where {@code here} puts the player (only Nyxara's sides differ: everywhere else it is the current column). */
    public enum View {
        STAY, DAYSIDE, BAND, NIGHTSIDE
    }

    /**
     * One sky state.
     *
     * @param name    the command word
     * @param phase   the clock phase to set and freeze, or {@link #KEEP_PHASE}
     * @param weather what happens to the weather
     * @param view    where {@code here} puts the player
     * @param yaw     the facing {@code here} sets (Minecraft yaw: 90 west, -90 east), or {@link #KEEP}
     * @param pitch   the pitch {@code here} sets (positive looks down), or {@link #KEEP}
     * @param sky     the look this state shows, for the sky renderer to key on later
     */
    public record State(String name, double phase, Weather weather, View view, float yaw, float pitch, String sky) {

        public boolean setsPhase() {
            return !Double.isNaN(this.phase);
        }

        public boolean setsFacing() {
            return !Float.isNaN(this.yaw) || !Float.isNaN(this.pitch);
        }
    }

    private static final Map<String, List<State>> TABLE;

    static {
        Map<String, List<State>> t = new LinkedHashMap<>();
        // Facings (used by "here") look at each state's set piece, matching the sky previews: yaw 90 west, -90 east,
        // 180 north, 0 south; a negative pitch looks up.
        t.put("greenxertz", with(day(),
                face("twin_noon", NOON, Weather.CLEAR, 0.0F, -62.0F, "twin_noon"),
                face("spore_bloom", NOON, Weather.START, 180.0F, -20.0F, "spore_bloom")));
        t.put("cindara", with(day(),
                face("eclipse", 0.45, Weather.CLEAR, 90.0F, -24.0F, "eclipse"),
                face("ash_fall", NOON, Weather.START, 180.0F, -20.0F, "ash_fall")));
        t.put("glacira", with(day(),
                face("twilight", 0.50, Weather.CLEAR, 90.0F, -12.0F, "twilight"),
                face("aurora", NIGHT, Weather.CLEAR, 180.0F, -28.0F, "aurora"),
                face("diamond_dust", NIGHT, Weather.START, 180.0F, -28.0F, "diamond_dust")));
        t.put("station", with(day(),
                face("sunrise", 0.0, Weather.CLEAR, -90.0F, 4.0F, "sunrise"),
                face("planet_night", NIGHT, Weather.CLEAR, -90.0F, 35.0F, "planet_night")));
        t.put("luna", with(day(),
                face("earthrise", 0.82, Weather.CLEAR, -90.0F, -12.0F, "earthrise")));
        t.put("mars", with(day(),
                face("blue_sunset", 0.50, Weather.CLEAR, 90.0F, -6.0F, "blue_sunset"),
                face("dust_storm", NOON, Weather.START, 180.0F, -15.0F, "dust_storm")));
        t.put("nyxara", List.of(
                place("dawn", View.BAND, KEEP, KEEP, "band"),
                place("noon", View.DAYSIDE, 90.0F, -15.0F, "dayside"),
                place("dusk", View.BAND, KEEP, KEEP, "band"),
                place("night", View.NIGHTSIDE, -90.0F, -15.0F, "nightside"),
                place("dayside", View.DAYSIDE, 90.0F, -15.0F, "dayside"),
                place("band", View.BAND, -135.0F, -18.0F, "band"),
                place("nightside", View.NIGHTSIDE, -90.0F, -15.0F, "nightside"),
                place("galaxy_side", View.NIGHTSIDE, -90.0F, -20.0F, "galaxy_side"),
                new State("flare", KEEP_PHASE, Weather.START, View.BAND, 90.0F, -12.0F, "flare")));
        TABLE = Collections.unmodifiableMap(t);
    }

    private GallerySkyStates() {
    }

    private static List<State> day() {
        return List.of(at("dawn", DAWN, "dawn"), at("noon", NOON, "noon"), at("dusk", DUSK, "dusk"),
                at("night", NIGHT, "night"));
    }

    @SafeVarargs
    private static <T> List<T> with(List<T> base, T... more) {
        List<T> all = new ArrayList<>(base);
        Collections.addAll(all, more);
        return List.copyOf(all);
    }

    private static State at(String name, double phase, String sky) {
        return new State(name, phase, Weather.CLEAR, View.STAY, KEEP, KEEP, sky);
    }

    private static State face(String name, double phase, Weather weather, float yaw, float pitch, String sky) {
        return new State(name, phase, weather, View.STAY, yaw, pitch, sky);
    }

    private static State place(String name, View view, float yaw, float pitch, String sky) {
        return new State(name, KEEP_PHASE, Weather.CLEAR, view, yaw, pitch, sky);
    }

    /** The worlds that have sky states, by lang id, in the stable public planet order. */
    public static Set<String> planets() {
        return TABLE.keySet();
    }

    /** The states of {@code planet} (lang id), empty for an unknown world. */
    public static List<State> states(String planet) {
        List<State> s = planet == null ? null : TABLE.get(planet.toLowerCase(Locale.ROOT));
        return s == null ? List.of() : s;
    }

    /** The state names of {@code planet}, in table order (what tab-completion offers). */
    public static List<String> names(String planet) {
        return states(planet).stream().map(State::name).toList();
    }

    /** Parses a state word for {@code planet} (case-insensitive). */
    public static Optional<State> find(String planet, String state) {
        if (state == null) {
            return Optional.empty();
        }
        String want = state.toLowerCase(Locale.ROOT);
        return states(planet).stream().filter(s -> s.name().equals(want)).findFirst();
    }

    /**
     * The block X a Nyxara viewing spot uses: the band keeps {@code x} when it is already well inside the band
     * (otherwise the band centre side of it), and a side moves {@link #SIDE_CLEARANCE} blocks past the ragged
     * terminator into the dayside (west, -X) or nightside (east, +X). {@link View#STAY} keeps {@code x}.
     */
    public static int viewX(View view, int x, int width, int originX, int edgeNoise) {
        int half = FixedXBands.clampWidth(width) / 2;
        int noise = Math.max(0, Math.min(FixedXBands.MAX_EDGE_NOISE, edgeNoise));
        return switch (view) {
            case STAY -> x;
            case BAND -> za.co.neroland.nerospace.item.CompassLanding.intoBand(x, width, originX, edgeNoise);
            case DAYSIDE -> originX - half - noise - SIDE_CLEARANCE;
            case NIGHTSIDE -> originX + half + noise + SIDE_CLEARANCE;
        };
    }
}
