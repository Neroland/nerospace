package za.co.neroland.nerospace.rocket;

import java.util.Locale;

/**
 * Pure descent state machine numbers and transitions (005 §3). The server drives {@link RocketEntity}'s
 * {@code Phase}; the client only renders it.
 */
public final class DescentMath {

    /** The arriving rocket spawns this far above its final pose. */
    public static final int DESCENT_HEIGHT = 48;
    /** Blocks per tick: 48 / 0.8 = 60 ticks nominal. */
    public static final double DESCENT_SPEED = 0.8D;
    /** Hard cap on {@code DESCENDING}; past it the rocket is snapped down ({@code ABORTED}). */
    public static final int DESCENT_TIMEOUT = 200;
    /** Ticks from {@code TOUCHDOWN} to {@code LANDED}. */
    public static final int SETTLE_TICKS = 8;
    /** The target block under the spawn is re-checked this often while descending. */
    public static final int PAD_CHECK_INTERVAL = 10;
    /** Horizontal slide speed to a re-resolved target (cosmetic, high above the ground). */
    public static final double SLIDE_SPEED = 2.0D;
    /** An aborted landing whose spawn is blocked is pushed up at most this far. */
    public static final int ABORT_MAX_PUSH = 8;
    /** A rider may not launch again within this many ticks of their previous launch (005 G5). */
    public static final int LAUNCH_COOLDOWN = 100;

    public enum Phase {
        IDLE, ASCENDING, DESCENDING, TOUCHDOWN, LANDED, ABORTED;

        /** Lenient by-name lookup: absent/unknown → {@link #IDLE} (a 1.6.0 rocket). */
        public static Phase byName(String name) {
            if (name == null || name.isEmpty()) {
                return IDLE;
            }
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return IDLE;
            }
        }
    }

    private DescentMath() {
    }

    /** Nominal descent length in ticks (60). */
    public static int nominalTicks() {
        return (int) Math.ceil(DESCENT_HEIGHT / DESCENT_SPEED);
    }

    /**
     * One server tick of {@code DESCENDING}: the next phase given the ticks already spent and the current
     * height above the spawn. Reaching the spawn → {@code TOUCHDOWN}; the timeout → {@code ABORTED}.
     */
    public static Phase step(int phaseTick, double heightAboveSpawn) {
        if (heightAboveSpawn <= 0.0D) {
            return Phase.TOUCHDOWN;
        }
        if (phaseTick >= DESCENT_TIMEOUT) {
            return Phase.ABORTED;
        }
        return Phase.DESCENDING;
    }

    /** The phase a rocket saved in {@code saved} resumes in on load: a mid-air descent finishes at once. */
    public static Phase onLoad(Phase saved) {
        return switch (saved) {
            case DESCENDING, ABORTED -> Phase.ABORTED;
            case TOUCHDOWN, LANDED -> Phase.IDLE;
            default -> saved;
        };
    }

    /** Whether the target under the spawn is checked on this descent tick. */
    public static boolean checkPadThisTick(int phaseTick) {
        return phaseTick > 0 && phaseTick % PAD_CHECK_INTERVAL == 0;
    }

    /** The reversed ascent animation counter (client): {@code LAUNCH_DURATION × remaining / 48}. */
    public static float reversedLaunchAnim(int launchDuration, double heightAboveSpawn) {
        double f = Math.max(0.0D, Math.min(1.0D, heightAboveSpawn / DESCENT_HEIGHT));
        return (float) (launchDuration * f);
    }
}
