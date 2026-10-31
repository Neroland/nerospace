package za.co.neroland.nerospace.block.rules;

/** Shard Drift: a living entity standing in it with no boots at all takes 1.0 damage every 20 ticks. Any boots stop it. */
public final class SharpRules {

    public static final float DAMAGE = 1.0F;
    public static final int INTERVAL_TICKS = 20;

    private SharpRules() {
    }

    public static boolean cuts(boolean living, boolean anyBoots) {
        return living && !anyBoots;
    }

    public static boolean dueThisTick(long gameTime) {
        return gameTime % INTERVAL_TICKS == 0;
    }
}
