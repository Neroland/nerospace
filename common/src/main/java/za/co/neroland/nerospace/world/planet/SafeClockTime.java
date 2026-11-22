package za.co.neroland.nerospace.world.planet;

import java.util.function.LongSupplier;

import net.minecraft.world.level.Level;

/**
 * Day time for a level that mirrors the vanilla clock. {@code Level.getDefaultClockTime()} is sun-accurate on 26.x,
 * but it throws when that clock's time markers are not loaded ("Time marker ... does not exist"). The first throw
 * sticks for the rest of the process and every later read uses {@code getGameTime()}, the same fallback
 * {@code SolarPanelRenderer} used on its own. Below 26.1 the call is rewritten to {@code getDayTime()}, which does
 * not throw, so the try is a no-op there.
 */
public final class SafeClockTime {

    /** Once false, the throwing clock is never called again. */
    private static volatile boolean available = true;

    private SafeClockTime() {
    }

    /** The level's clock time, or its game time after the clock has failed once. */
    public static long of(Level level) {
        return read(() -> level.getDefaultClockTime(), level::getGameTime);
    }

    /**
     * Tries {@code clock} while it has not yet failed. A {@link RuntimeException} switches every later call,
     * including this one, over to {@code game}.
     */
    static long read(LongSupplier clock, LongSupplier game) {
        if (available) {
            try {
                return clock.getAsLong();
            } catch (RuntimeException ex) {
                available = false;
            }
        }
        return game.getAsLong();
    }

    /** Test hook. Production never resets: a missing marker stays missing until the process restarts. */
    static void resetForTest() {
        available = true;
    }
}
