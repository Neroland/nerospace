package za.co.neroland.nerospace.world.station;

import java.util.Locale;

import com.mojang.serialization.Codec;

/**
 * A station's class — its one trait. Persisted <b>by name</b> (never ordinal); an unknown or
 * missing name reads as {@link #WAYSTATION}, so every 1.x station loads as a Waystation.
 */
public enum StationClass {
    WAYSTATION(new StationTraits(9, 1.00, 1.00, 1.00, false)),
    RESEARCH(new StationTraits(8, 1.25, 1.00, 1.00, true)),
    INDUSTRIAL(new StationTraits(8, 1.00, 1.50, 0.50, false)),
    TRADE(new StationTraits(10, 1.00, 1.00, 1.00, false));

    /** Lenient by-name codec: unknown names decode to {@link #WAYSTATION}. */
    public static final Codec<StationClass> CODEC = Codec.STRING.xmap(StationClass::parse, StationClass::name);

    private final StationTraits traits;

    StationClass(StationTraits traits) {
        this.traits = traits;
    }

    public StationTraits traits() {
        return this.traits;
    }

    /** Lower-case id used in lang keys ({@code gui.nerospace.station_console.class.<id>}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The next class in console cycle order (wraps); {@code backwards} cycles the other way. */
    public StationClass cycle(boolean backwards) {
        StationClass[] all = values();
        int n = all.length;
        return all[(ordinal() + (backwards ? n - 1 : 1)) % n];
    }

    /** Lenient parse: {@code null}, blank or unknown → {@link #WAYSTATION}. */
    public static StationClass parse(String name) {
        if (name == null) {
            return WAYSTATION;
        }
        for (StationClass c : values()) {
            if (c.name().equalsIgnoreCase(name.trim())) {
                return c;
            }
        }
        return WAYSTATION;
    }

    /** By ordinal for the console's synced data; out of range → {@link #WAYSTATION}. */
    public static StationClass byOrdinal(int ordinal) {
        StationClass[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : WAYSTATION;
    }
}
