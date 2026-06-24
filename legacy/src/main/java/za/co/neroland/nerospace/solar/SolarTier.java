package za.co.neroland.nerospace.solar;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import za.co.neroland.nerospace.Tuning;

/**
 * The three solar-panel tiers. Each tier is its own registered block, occupies an {@code NxN}
 * horizontal footprint, and only ever pools energy with panels of the SAME tier (a Tier 1 array and a
 * Tier 2 array never merge — see {@link SolarArray}). Generation and storage come from {@link Tuning}
 * (config-scaled), so modpacks tune the whole family through {@code energyRateMultiplier}.
 *
 * <p>VERTICAL SLICE: only {@link #TIER_1} is registered/wired so far; the Tier 2/3 entries exist so the
 * balance numbers, recipes and renderer are already tier-aware when those blocks are added.</p>
 */
public enum SolarTier implements StringRepresentable {
    TIER_1("tier_1", 1, 1),
    TIER_2("tier_2", 2, 2),
    TIER_3("tier_3", 3, 3);

    public static final Codec<SolarTier> CODEC = StringRepresentable.fromEnum(SolarTier::values);

    private final String name;
    /** 1-based tier number (drives the Tuning lookups). */
    public final int tier;
    /** Footprint edge length in blocks: T1 = 1 (1x1), T2 = 2 (2x2), T3 = 3 (3x3). */
    public final int footprint;

    SolarTier(String name, int tier, int footprint) {
        this.name = name;
        this.tier = tier;
        this.footprint = footprint;
    }

    /** Peak FE/tick this single panel adds at full sun (config-scaled). */
    public int fePerTick() {
        return Tuning.solarPanelFePerTick(this.tier);
    }

    /** This panel's own FE buffer; an array's total storage is the sum across its members. */
    public int buffer() {
        return Tuning.solarPanelBuffer(this.tier);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
