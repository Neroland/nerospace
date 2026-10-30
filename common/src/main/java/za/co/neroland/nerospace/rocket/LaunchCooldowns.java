package za.co.neroland.nerospace.rocket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The per-rider launch cooldown (005 G5): rider UUID → game time the next launch is allowed. In memory only, never
 * saved; expired entries are dropped on every launch, so the map holds at most the riders who launched in the last
 * {@link DescentMath#LAUNCH_COOLDOWN} ticks. POPIA/GDPR erasure drops a player's entry ({@link #forget}).
 */
public final class LaunchCooldowns {

    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    private LaunchCooldowns() {
    }

    /**
     * @return {@code false} if the rider is still cooling down; otherwise records the launch and returns {@code true}
     */
    public static synchronized boolean tryStart(UUID rider, long now, long cooldown) {
        Long until = UNTIL.get(rider);
        if (until != null && now < until) {
            return false;
        }
        UNTIL.entrySet().removeIf(e -> e.getValue() <= now);
        UNTIL.put(rider, now + cooldown);
        return true;
    }

    /** Whether {@code rider} is still inside the cooldown window at {@code now}. Read-only. */
    public static synchronized boolean coolingDown(UUID rider, long now) {
        Long until = UNTIL.get(rider);
        return until != null && now < until;
    }

    /** What {@link #gate} decided. {@code refusal} is the fee's refusal key when {@code verdict} is REFUSED. */
    public record Gate(Verdict verdict, @org.jetbrains.annotations.Nullable String refusal) {
    }

    public enum Verdict { COOLDOWN, REFUSED, OK }

    /**
     * The launch order around a fee (005 G5): the cooldown is checked first, so a launch inside the window is
     * refused before {@code charge} runs and nothing is paid; then the fee is charged ({@code charge} returns a
     * refusal key or {@code null}); the launch is recorded only when the fee passed.
     */
    public static Gate gate(UUID rider, long now, long cooldown,
            java.util.function.Supplier<String> charge) {
        if (coolingDown(rider, now)) {
            return new Gate(Verdict.COOLDOWN, null);
        }
        String refused = charge.get();
        if (refused != null) {
            return new Gate(Verdict.REFUSED, refused);
        }
        tryStart(rider, now, cooldown);
        return new Gate(Verdict.OK, null);
    }

    /** Erasure: drops the player's entry. */
    public static synchronized boolean forget(UUID player) {
        return UNTIL.remove(player) != null;
    }

    /** Conformance probe: whether an entry exists for the player. */
    public static synchronized boolean holds(UUID player) {
        return UNTIL.containsKey(player);
    }
}
