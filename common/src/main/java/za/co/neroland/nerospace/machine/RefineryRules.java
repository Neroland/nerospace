package za.co.neroland.nerospace.machine;

import za.co.neroland.nerospace.block.rules.OxidiserRules;

/**
 * Pure Fuel Refinery maths (no Minecraft types, JUnit-tested): the Mars perchlorate oxidiser boost and the Luna
 * raw ilmenite line.
 *
 * <p>Oxidiser: one Perchlorate Oxidiser covers {@link OxidiserRules#BUCKETS_PER_OXIDISER} buckets of
 * base output; while covered, a batch yields {@link OxidiserRules#MULTIPLIER} times the base amount.</p>
 *
 * <p>Ilmenite (D3): one Raw Ilmenite becomes one Titanium Ingot plus {@link #ILMENITE_OXYGEN_MB} mB of oxygen.</p>
 */
public final class RefineryRules {

    /** Base mB of output one oxidiser covers (four buckets). */
    public static final int OXIDISER_COVERAGE_MB = OxidiserRules.BUCKETS_PER_OXIDISER * 1_000;
    /** Oxygen released per raw ilmenite refined. */
    public static final int ILMENITE_OXYGEN_MB = 250;
    /** Titanium ingots per raw ilmenite. */
    public static final int TITANIUM_PER_ILMENITE = 1;

    private RefineryRules() {
    }

    /** mB a batch adds to the tank: the base batch, raised by the oxidiser multiplier when boosted. */
    public static int batchMb(int baseBatchMb, boolean boosted) {
        return (int) Math.round(baseBatchMb * OxidiserRules.yieldMultiplier(boosted));
    }

    /** Whether a batch about to complete is boosted: coverage left, or a fresh oxidiser waiting in the slot. */
    public static boolean boosted(int coverageMb, boolean oxidiserInSlot) {
        return coverageMb > 0 || oxidiserInSlot;
    }

    /** Whether a fresh oxidiser must be consumed for the next batch (no coverage left, one in the slot). */
    public static boolean consumesOxidiser(int coverageMb, boolean oxidiserInSlot) {
        return coverageMb <= 0 && oxidiserInSlot;
    }

    /** Coverage left after a boosted batch of {@code baseBatchMb} base output. */
    public static int coverageAfterBatch(int coverageMb, int baseBatchMb) {
        return Math.max(0, coverageMb - baseBatchMb);
    }

    /** Whether the titanium output slot can take one more refined ingot. */
    public static boolean titaniumFits(boolean outputEmpty, boolean outputIsTitanium, int outputCount, int maxStack) {
        return outputEmpty || (outputIsTitanium && outputCount + TITANIUM_PER_ILMENITE <= maxStack);
    }
}
