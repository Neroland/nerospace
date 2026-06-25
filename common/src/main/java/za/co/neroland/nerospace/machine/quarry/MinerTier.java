package za.co.neroland.nerospace.machine.quarry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Quarry progression tiers. A tier gates the largest square the quarry may claim ({@link #maxAreaSide}),
 * how many upgrade-module slots it has ({@link #moduleSlots}), and the base per-cycle work ceiling
 * ({@link #baseBlocksPerCycle}). Planets are gated independently: Cindara wants Tier 2, Glacira wants
 * Tier 3; everything else runs at Tier 1.
 */
public enum MinerTier {

    TIER_1(1, 64, 1, 2, 0xFFE0405A),
    TIER_2(2, 64, 2, 4, 0xFFB060E0),
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

    public int level() {
        return this.level;
    }

    public int maxAreaSide() {
        return this.maxAreaSide;
    }

    public int moduleSlots() {
        return this.moduleSlots;
    }

    public int baseBlocksPerCycle() {
        return this.baseBlocksPerCycle;
    }

    public int accentColor() {
        return this.accentColor;
    }

    public boolean canOperateIn(@org.jspecify.annotations.NonNull ResourceKey<Level> dimension) {
        return this.level >= requiredTier(dimension);
    }

    public static int requiredTier(@org.jspecify.annotations.NonNull ResourceKey<Level> dimension) {
        if (dimension.equals(ModDimensions.GLACIRA_LEVEL)) {
            return 3;
        }
        if (dimension.equals(ModDimensions.CINDARA_LEVEL)) {
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
