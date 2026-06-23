package za.co.neroland.nerospace.machine.quarry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Quarry progression tiers (MINER_DESIGN). A tier gates three things: the largest square the quarry
 * may claim ({@link #maxAreaSide}), how many upgrade-module slots it has ({@link #moduleSlots}), and
 * the base per-cycle work ceiling ({@link #baseBlocksPerCycle}) — actual throughput scales with the
 * power fed in, up to that ceiling × the modules' speed multiplier.
 *
 * <p>Planets are gated independently of the tier list (the quarry otherwise runs anywhere it has
 * power): the harsh outer moons need a higher tier — Cindara wants Tier 2, Glacira wants Tier 3 —
 * while the Overworld, Greenxertz, the Station and any unlisted dimension run at Tier 1. Per-planet
 * speed/yield differences live in {@link PlanetMiningProfile}.</p>
 *
 * <p>Tier 1 is the only one with content this slice; Tier 2/3 are scaffolded here so adding them is
 * just a block + recipe + texture.</p>
 */
public enum MinerTier {

    TIER_1(1, 16, 1, 2, 0xFFE0405A),
    TIER_2(2, 32, 2, 4, 0xFFB060E0),
    TIER_3(3, 64, 4, 8, 0xFFE0C040);

    private final int level;
    private final int maxAreaSide;
    private final int moduleSlots;
    private final int baseBlocksPerCycle;
    private final int accentColor;

    MinerTier(int level, int maxAreaSide, int moduleSlots, int baseBlocksPerCycle, int accentColor) {
        this.level = level;
        this.maxAreaSide = maxAreaSide;
        this.moduleSlots = moduleSlots;
        this.baseBlocksPerCycle = baseBlocksPerCycle;
        this.accentColor = accentColor;
    }

    /** Human-facing tier number (1-based). */
    public int level() {
        return this.level;
    }

    /** Longest side (blocks) of the rectangle this tier may claim. */
    public int maxAreaSide() {
        return this.maxAreaSide;
    }

    public int moduleSlots() {
        return this.moduleSlots;
    }

    /** Base per-work-cycle block ceiling (before the modules' speed multiplier). */
    public int baseBlocksPerCycle() {
        return this.baseBlocksPerCycle;
    }

    /** GUI / drill-head accent (ARGB): red → purple → gold across the tiers. */
    public int accentColor() {
        return this.accentColor;
    }

    /** Whether a quarry of this tier may operate in {@code dimension}. */
    public boolean canOperateIn(ResourceKey<Level> dimension) {
        return this.level >= requiredTier(dimension);
    }

    /** The minimum quarry tier needed to mine in {@code dimension} (1 for everything but the moons). */
    public static int requiredTier(ResourceKey<Level> dimension) {
        if (ModDimensions.GLACIRA_LEVEL.equals(dimension)) {
            return 3;
        }
        if (ModDimensions.CINDARA_LEVEL.equals(dimension)) {
            return 2;
        }
        return 1;
    }

    public static MinerTier byOrdinal(int ordinal) {
        MinerTier[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return TIER_1;
        }
        return values[ordinal];
    }
}
