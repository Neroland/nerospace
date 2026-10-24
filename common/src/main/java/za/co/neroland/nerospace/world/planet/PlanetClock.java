package za.co.neroland.nerospace.world.planet;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerospace.network.PlanetClockPayload;

/**
 * The server-authoritative planet clock. One phase per planet level, advanced per the planet's day length and
 * stored in {@link PlanetClockState}. Greenxertz ({@link ClockSource#LEVEL_TIME}) mirrors its vanilla day time and
 * never writes. Vanilla time on every other planet stays frozen, so block light, sky light and mob spawning never
 * follow this clock — it drives the sky, the sun, solar output and villagers.
 *
 * <p>The clock starts on its first read (or first tick), whether or not a player is present, so unattended
 * force-loaded machinery sees a moving clock. Reads are memoised per level and game tick, so hundreds of solar
 * panels cost one saved-data lookup per planet per tick.</p>
 *
 * <p>Synced to a client on join, dimension change and ops changes only (never per tick); the client recomputes
 * the phase from its own synced game time ({@code ClientPlanetClock}).</p>
 */
public final class PlanetClock {

    /** One-entry-per-level memo of the phase for the current game tick. Weak keys: no level is kept alive. */
    private static final Map<ServerLevel, long[]> MEMO_TIME = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ServerLevel, Double> MEMO_PHASE = Collections.synchronizedMap(new WeakHashMap<>());

    private PlanetClock() {
    }

    /** The planet's clock phase in [0,1) at {@code level}, or noon (0.25) for a non-planet level. */
    public static double phase(Level level) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return ClockMath.NOON;
        }
        if (planet.clockSource() == ClockSource.LEVEL_TIME) {
            return ClockMath.mirrorPhase(level.getDefaultClockTime());
        }
        if (!NerospaceConfig.clockEnabled()) {
            return ClockMath.NOON;
        }
        if (!(level instanceof ServerLevel server)) {
            return planet.pinnedPhase();
        }
        long now = level.getGameTime();
        long[] t = MEMO_TIME.get(server);
        if (t != null && t[0] == now) {
            Double cached = MEMO_PHASE.get(server);
            if (cached != null) {
                return cached;
            }
        }
        PlanetClockState s = PlanetClockState.get(server);
        boolean started = s.started();
        double phase = s.readPhase(now, planet.traits().dayLengthTicks(), planet.pinnedPhase());
        MEMO_TIME.put(server, new long[] {now});
        MEMO_PHASE.put(server, phase);
        if (!started) {
            syncLevel(server);
        }
        return phase;
    }

    /** Daylight in [0,1] at {@code level} (curve × weather is the caller's concern). */
    public static double daylight(Level level) {
        PlanetDefinition planet = Planets.get(level.dimension());
        return planet == null ? 1.0 : planet.daylight().at(phase(level));
    }

    /** The 1-based planet day ("Sol N" on Mars), counted from the clock's start. */
    public static long dayNumber(ServerLevel level) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null || planet.clockSource() == ClockSource.LEVEL_TIME) {
            return 1L + Math.floorDiv(level.getDefaultClockTime(), ClockMath.VANILLA_DAY);
        }
        return PlanetClockState.get(level).readDay(level.getGameTime(), planet.traits().dayLengthTicks());
    }

    /** Drops the memo for a level (after an ops change). */
    private static void invalidate(ServerLevel level) {
        MEMO_TIME.remove(level);
        MEMO_PHASE.remove(level);
    }

    /** Per-server tick: starts every loaded planet clock (players present or not). O(1) per planet level. */
    public static void tick(MinecraftServer server) {
        for (PlanetDefinition planet : Planets.ALL) {
            if (planet.clockSource() != ClockSource.PLANET_CLOCK) {
                continue;
            }
            ServerLevel level = server.getLevel(planet.dimension());
            if (level != null && PlanetClockState.get(level).initialise(level.getGameTime())) {
                invalidate(level);
                syncLevel(level);
            }
        }
    }

    // --- Ops -------------------------------------------------------------------------------------------

    /** Sets the clock so its phase is {@code target} now. @return false for a non-planet level. */
    public static boolean set(ServerLevel level, double target) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return false;
        }
        double wrapped = DaylightCurve.wrap(target);
        if (planet.clockSource() == ClockSource.LEVEL_TIME) {
            // Greenxertz: same as /time set on its clock (shared with the Overworld, exactly as /time is).
            Optional<Holder<WorldClock>> clock = level.dimensionType().defaultClock();
            if (clock.isEmpty()) {
                return false;
            }
            long dayStart = Math.floorDiv(level.getDefaultClockTime(), ClockMath.VANILLA_DAY) * ClockMath.VANILLA_DAY;
            long ticks = dayStart + Math.round(wrapped * ClockMath.VANILLA_DAY);
            level.getServer().clockManager().setTotalTicks(clock.get(), ticks);
            syncLevel(level);
            return true;
        }
        PlanetClockState s = PlanetClockState.get(level);
        s.initialise(level.getGameTime());
        if (planet.traits().fixedClock()) {
            s.pinnedOverride = wrapped;
        } else {
            long now = s.frozen ? s.frozenAt : level.getGameTime();
            s.offsetTicks = ClockMath.offsetFor(now, s.epochTicks, planet.traits().dayLengthTicks(), wrapped);
        }
        s.markChanged();
        invalidate(level);
        syncLevel(level);
        return true;
    }

    /** Freezes or thaws a PLANET_CLOCK planet. @return false when not applicable. */
    public static boolean setFrozen(ServerLevel level, boolean frozen) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null || planet.clockSource() != ClockSource.PLANET_CLOCK || planet.traits().fixedClock()) {
            return false;
        }
        PlanetClockState s = PlanetClockState.get(level);
        s.initialise(level.getGameTime());
        if (frozen == s.frozen) {
            return true;
        }
        long now = level.getGameTime();
        if (frozen) {
            s.frozenAt = now;
        } else {
            // Shift the timeline so the phase resumes from where it stopped.
            s.offsetTicks += now - s.frozenAt;
        }
        s.frozen = frozen;
        s.markChanged();
        invalidate(level);
        syncLevel(level);
        return true;
    }

    // --- Sync ------------------------------------------------------------------------------------------

    /** The clock snapshot for a level, or null when the level is not a planet. */
    @Nullable
    public static PlanetClockPayload snapshot(ServerLevel level) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return null;
        }
        if (planet.clockSource() == ClockSource.LEVEL_TIME) {
            return new PlanetClockPayload(planet.dimension().identifier(), ClockMath.VANILLA_DAY, 0L, 0L,
                    (float) planet.pinnedPhase(), true, false, 0L, 1L);
        }
        PlanetClockState s = PlanetClockState.get(level);
        s.initialise(level.getGameTime());
        boolean on = NerospaceConfig.clockEnabled();
        long dayLength = on ? planet.traits().dayLengthTicks() : 0L;
        double pinned = s.pinnedOverride >= 0.0 ? s.pinnedOverride : planet.pinnedPhase();
        return new PlanetClockPayload(planet.dimension().identifier(), dayLength, s.epochTicks, s.offsetTicks,
                on ? (float) pinned : (float) ClockMath.NOON, false, s.frozen, s.frozenAt, dayNumber(level));
    }

    /** Sends the current level's clock to one player (join / dimension change). */
    public static void syncTo(ServerPlayer player) {
        PlanetClockPayload payload = snapshot(player.level());
        ModNetwork.sendToPlayer(player, payload != null ? payload
                : PlanetClockPayload.none(player.level().dimension().identifier()));
    }

    /** Re-sends the clock to every player in the level. */
    public static void syncLevel(ServerLevel level) {
        if (level.players().isEmpty()) {
            return;
        }
        PlanetClockPayload payload = snapshot(level);
        if (payload == null) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            ModNetwork.sendToPlayer(player, payload);
        }
    }
}
