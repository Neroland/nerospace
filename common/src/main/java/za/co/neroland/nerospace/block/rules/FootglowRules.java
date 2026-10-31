package za.co.neroland.nerospace.block.rules;

/** Rose Crystal: a player (not a mob, not a spectator) stepping on it lights it to 9 for 60 ticks; idle light 2. */
public final class FootglowRules {

    public static final int LIT_TICKS = 60;
    public static final int LIT_LIGHT = 9;
    public static final int IDLE_LIGHT = 2;

    private FootglowRules() {
    }

    /** Whether a step changes the block; an already lit block is left alone (no re-schedule). */
    public static boolean lightUp(boolean player, boolean spectator, boolean alreadyLit) {
        return player && !spectator && !alreadyLit;
    }

    public static int light(boolean lit) {
        return lit ? LIT_LIGHT : IDLE_LIGHT;
    }
}
