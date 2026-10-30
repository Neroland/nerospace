package za.co.neroland.nerospace.block.rules;

/** Stripped Bio-Bark Log: waking within 4 blocks of one heals 4 HP and gives Regeneration I 10 s, once. */
public final class HealBedRules {

    public static final int RADIUS = 4;
    public static final float HEAL = 4.0F;
    public static final int REGEN_TICKS = 200;

    private HealBedRules() {
    }

    public static float healOnWake(int strippedLogsWithin4) {
        return strippedLogsWithin4 >= 1 ? HEAL : 0.0F;
    }
}
