package za.co.neroland.nerospace.client;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.network.PlanetClockPayload;
import za.co.neroland.nerospace.world.planet.ClockMath;

/**
 * Client-side copy of the current level's planet clock, filled by {@link PlanetClockPayload}. A pure data
 * holder (no client-only imports) so the handler can be registered from common code. The phase is recomputed
 * from the client's own synced game time, so the sky moves smoothly at any TPS.
 */
public final class ClientPlanetClock {

    @Nullable
    private static volatile PlanetClockPayload current;

    private ClientPlanetClock() {
    }

    public static void accept(PlanetClockPayload payload) {
        // Server data is trusted, but a non-finite phase would poison every curve: drop it.
        current = payload.isPlanet() && Float.isFinite(payload.pinnedPhase()) ? payload : null;
    }

    /** Whether the client is on a planet with a known clock. */
    public static boolean known(@Nullable Level level) {
        PlanetClockPayload c = current;
        return c != null && level != null && c.dimension().equals(level.dimension().identifier());
    }

    /** The current planet phase in [0,1), or noon when unknown. */
    public static double phase(@Nullable Level level) {
        PlanetClockPayload c = current;
        if (c == null || level == null || !c.dimension().equals(level.dimension().identifier())) {
            return ClockMath.NOON;
        }
        if (c.mirrorVanilla()) {
            return ClockMath.mirrorPhase(level.getDefaultClockTime());
        }
        return ClockMath.phase(level.getGameTime(), c.epochTicks(), c.offsetTicks(), c.dayLength(),
                c.pinnedPhase(), c.frozen(), c.frozenAt());
    }

    /**
     * Continuous planet days (whole days plus {@link #phase}) for things that move across days, such as the sky's
     * orbiting bodies; its fractional part is {@link #phase}. Noon of day 0 when unknown.
     */
    public static double days(@Nullable Level level) {
        PlanetClockPayload c = current;
        if (c == null || level == null || !c.dimension().equals(level.dimension().identifier())) {
            return ClockMath.NOON;
        }
        if (c.mirrorVanilla()) {
            return ClockMath.mirrorDays(level.getDefaultClockTime());
        }
        return ClockMath.days(level.getGameTime(), c.epochTicks(), c.offsetTicks(), c.dayLength(), c.pinnedPhase(),
                c.frozen(), c.frozenAt());
    }

    /**
     * Nyxara's twilight band width as the server bound it for this world (an existing world keeps the width it was
     * created with), or {@code fallback} when unknown.
     */
    public static int bandWidth(@Nullable Level level, int fallback) {
        PlanetClockPayload c = current;
        if (c == null || level == null || c.bandWidth() <= 0 || !c.dimension().equals(level.dimension().identifier())) {
            return fallback;
        }
        return c.bandWidth();
    }

    /** The planet day ("Sol N" on Mars) as last synced, or 0 when unknown. */
    public static long dayNumber() {
        PlanetClockPayload c = current;
        return c == null ? 0L : c.dayNumber();
    }

    /** Whether the planet's clock has a day length (the HUD clock dial only shows then). */
    public static boolean hasDayLength() {
        PlanetClockPayload c = current;
        return c != null && (c.mirrorVanilla() || c.dayLength() > 0L);
    }
}
