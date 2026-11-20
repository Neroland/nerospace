package za.co.neroland.nerospace.machine;

import za.co.neroland.nerospace.block.rules.OxidiserRules;

/**
 * Pure Fuel Refinery maths (no Minecraft types, JUnit-tested): the Mars perchlorate oxidiser boost and the Luna
 * raw ilmenite line.
 *
 * <p>Oxidiser: one Perchlorate Oxidiser covers {@link OxidiserRules#BUCKETS_PER_OXIDISER} buckets of
 * base output; while covered, a batch yields {@link OxidiserRules#MULTIPLIER} times the base amount.</p>
 *
 * <p>Ilmenite: one Raw Ilmenite becomes one Titanium Ingot plus {@link #ILMENITE_OXYGEN_MB} mB of oxygen.</p>
 *
 * <p>Oxidant Crystal (Europa), in the same slot: {@link #OXIDANT_OXYGEN_MB} mB of oxygen and no ingot.</p>
 */
public final class RefineryRules {

    /** Base mB of output one oxidiser covers (four buckets). */
    public static final int OXIDISER_COVERAGE_MB = OxidiserRules.BUCKETS_PER_OXIDISER * 1_000;
    /** Oxygen released per raw ilmenite refined. */
    public static final int ILMENITE_OXYGEN_MB = 250;
    /** Titanium ingots per raw ilmenite. */
    public static final int TITANIUM_PER_ILMENITE = 1;
    /** Oxygen released per Oxidant Crystal refined (Europa); a crystal makes no ingot. */
    public static final int OXIDANT_OXYGEN_MB = za.co.neroland.nerospace.world.planet.europa.EuropaNumbers.OXIDANT_OXYGEN_MB;

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

    /**
     * Whether an Oxidant Crystal may be refined: only when the oxygen buffer has room for all of its oxygen, so a
     * crystal is never spent on gas that would vent. It needs no room in the titanium slot.
     */
    public static boolean canRefineOxidant(long bufferFreeMb) {
        return bufferFreeMb >= OXIDANT_OXYGEN_MB;
    }

    /** Oxygen one refined input gives: a crystal's 400 mB or ilmenite's 250 mB. */
    public static int oxygenFor(boolean oxidantCrystal) {
        return oxidantCrystal ? OXIDANT_OXYGEN_MB : ILMENITE_OXYGEN_MB;
    }

    /** Titanium ingots one refined input gives: none for a crystal. */
    public static int titaniumFor(boolean oxidantCrystal) {
        return oxidantCrystal ? 0 : TITANIUM_PER_ILMENITE;
    }

    /** Whether the titanium output slot can take one more refined ingot. */
    public static boolean titaniumFits(boolean outputEmpty, boolean outputIsTitanium, int outputCount, int maxStack) {
        return outputEmpty || (outputIsTitanium && outputCount + TITANIUM_PER_ILMENITE <= maxStack);
    }
}
