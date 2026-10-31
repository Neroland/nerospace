package za.co.neroland.nerospace.world.planet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerolandcore.data.SavedDataCompat;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Per-level persisted state of a planet's clock ({@code nerospace:planet_clock}). Every field is optional with
 * a default, so a world that never had the file (every 1.x save) starts the clock on its first tick after the
 * upgrade. Holds no player data.
 *
 * <p>Never created for a {@link ClockSource#LEVEL_TIME} planet (Greenxertz mirrors vanilla time and writes
 * nothing).</p>
 */
public final class PlanetClockState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "planet_clock");
    public static final String RECOVERY_NAME = "nerospace:planet_clock";

    public static final SavedDataType<PlanetClockState> TYPE =
            SavedDataCompat.type(ID, PlanetClockState::new, codec());

    /** Game time the clock started at; {@code -1} until the first tick initialises it. */
    long epochTicks = -1L;
    /** Ops adjustment so a requested phase is reached. */
    long offsetTicks;
    boolean frozen;
    long frozenAt;
    /** Per-level override of a fixed clock's phase ({@code -1} = use the planet's pinned phase). */
    double pinnedOverride = -1.0;

    public PlanetClockState() {
    }

    public static PlanetClockState get(ServerLevel level) {
        return SavedDataRecovery.get(level, TYPE, PlanetClockState::new, RECOVERY_NAME);
    }

    private static Codec<PlanetClockState> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.optionalFieldOf("epoch_ticks", -1L).forGetter(s -> s.epochTicks),
                Codec.LONG.optionalFieldOf("offset_ticks", 0L).forGetter(s -> s.offsetTicks),
                Codec.BOOL.optionalFieldOf("frozen", false).forGetter(s -> s.frozen),
                Codec.LONG.optionalFieldOf("frozen_at", 0L).forGetter(s -> s.frozenAt),
                Codec.DOUBLE.optionalFieldOf("pinned_phase", -1.0).forGetter(s -> s.pinnedOverride),
                // Legacy pre-release field: accepted and ignored on read, never written (getter = default).
                Codec.LONG.optionalFieldOf("day_origin", -1L).forGetter(s -> -1L)
        ).apply(i, PlanetClockState::of));
    }

    private static PlanetClockState of(long epoch, long offset, boolean frozen, long frozenAt, double pinned,
            long ignoredDayOrigin) {
        PlanetClockState s = new PlanetClockState();
        s.epochTicks = epoch;
        s.offsetTicks = offset;
        s.frozen = frozen;
        s.frozenAt = frozenAt;
        s.pinnedOverride = pinned;
        return s;
    }

    /** Starts the clock at {@code gameTime} if it has never run. @return true if it changed. */
    boolean initialise(long gameTime) {
        if (this.epochTicks >= 0L) {
            return false;
        }
        this.epochTicks = gameTime;
        setDirty();
        return true;
    }

    /**
     * The phase at {@code gameTime}, starting the clock on this first read if it never ran (a force-loaded
     * planet with no player present still gets a moving clock). Day / "Sol N" counting starts at the epoch.
     *
     * @param dayLength    ticks per day, {@code 0} for a fixed clock
     * @param planetPinned the planet's pinned phase (a per-level ops override wins)
     */
    public double readPhase(long gameTime, long dayLength, double planetPinned) {
        initialise(gameTime);
        double pinned = this.pinnedOverride >= 0.0 ? this.pinnedOverride : planetPinned;
        return ClockMath.phase(gameTime, this.epochTicks, this.offsetTicks, dayLength, pinned, this.frozen,
                this.frozenAt);
    }

    /** The 1-based day number at {@code gameTime}, starting the clock if needed. */
    public long readDay(long gameTime, long dayLength) {
        initialise(gameTime);
        return ClockMath.dayNumber(gameTime, this.epochTicks, this.offsetTicks, dayLength, this.frozen, this.frozenAt);
    }

    /** Whether the clock has started (test/diagnostic). */
    public boolean started() {
        return this.epochTicks >= 0L;
    }

    void markChanged() {
        setDirty();
    }
}
