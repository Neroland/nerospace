package za.co.neroland.nerospace.world.station;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * Station module kinds. Persisted <b>by name</b>; {@link #CORE_LEGACY} is the first value.
 * Append-only: the Station Console addresses kinds by {@link #tile()}, which is {@code ordinal - 1} for every kind
 * after the two core kinds, so the order below must never change.
 */
public enum ModuleKind {
    /** The 1.x 7×7 room + airlock + pad. Recorded on first 2.0 visit; never built, repaired or removed. */
    CORE_LEGACY(1, null, true, false),
    /** The same room on a 2.0-founded station (rebuildable). */
    CORE(1, null, true, false),
    HABITAT_RING(1, null, true, true),
    DOCKING_RING(1, null, false, true),
    SOLAR_WING(2, null, false, true),
    HYDROPONICS_BAY(1, HABITAT_RING, true, true),
    GRAVITY_DECK(1, HABITAT_RING, true, true),
    OBSERVATORY(1, null, true, true);

    /** Console tiles: Core, Habitat, Docking, Solar Wing, Hydroponics, Gravity Deck, Observatory. */
    public static final int TILE_COUNT = 7;

    private final int maxCount;
    @Nullable
    private final ModuleKind requires;
    private final boolean airtight;
    private final boolean buildable;

    ModuleKind(int maxCount, @Nullable ModuleKind requires, boolean airtight, boolean buildable) {
        this.maxCount = maxCount;
        this.requires = requires;
        this.airtight = airtight;
        this.buildable = buildable;
    }

    /** How many of this kind a station may hold (Solar Wing 2, everything else 1). */
    public int maxCount() {
        return this.maxCount;
    }

    /** The module that must already exist before this one may be built, or {@code null}. */
    @Nullable
    public ModuleKind requires() {
        return this.requires;
    }

    /** Whether the module is a sealed room that gets one oxygen source cell. */
    public boolean airtight() {
        return this.airtight;
    }

    /** Whether the console's Build tab can build it (everything except the two core kinds). */
    public boolean buildable() {
        return this.buildable;
    }

    /** Whether it counts toward the eight-module cap (the core kinds do not). */
    public boolean countsTowardCap() {
        return this != CORE_LEGACY && this != CORE;
    }

    /** Console tile index. Both core kinds share tile 0. */
    public int tile() {
        return Math.max(0, ordinal() - 1);
    }

    /** Lower-case id for lang keys ({@code gui.nerospace.station_console.module.<id>}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The buildable kind on console tile {@code tile} (1..6), or {@code null}. */
    @Nullable
    public static ModuleKind byTile(int tile) {
        if (tile < 1 || tile >= TILE_COUNT) {
            return null;
        }
        return values()[tile + 1];
    }

    /** Strict parse: unknown → {@code null}. */
    @Nullable
    public static ModuleKind parse(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (ModuleKind k : values()) {
            if (k.name().equals(name)) {
                return k;
            }
        }
        return null;
    }
}
