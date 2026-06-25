package za.co.neroland.nerospace.machine;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;


import za.co.neroland.nerospace.config.NerospaceConfig;

/**
 * The three solar-panel tiers. Each tier is its own registered block and only ever pools energy with
 * panels of the SAME tier (a Tier 1 array and a Tier 2 array never merge — see {@link SolarArray}).
 * Generation and storage are config-scaled through {@code energyRateMultiplier}, so modpacks tune the
 * whole family at once.
 *
 * <p>Cross-loader port note: footprints are carried for parity with the standalone mod's N×N multiblock
 * tiers, but this slice registers every tier as a single 1×1 block (array pooling already gives the
 * "combine panels" feel); the N×N footprint + the sun-tracking renderer are a deferred enhancement. Base
 * values are inlined here (the standalone {@code Tuning} table isn't ported) and scaled at read time.</p>
 */
public enum SolarTier implements StringRepresentable {
    TIER_1("tier_1", 1, 1, 20, 50_000),
    TIER_2("tier_2", 2, 2, 100, 250_000),
    TIER_3("tier_3", 3, 3, 400, 1_000_000);

    public static final Codec<SolarTier> CODEC = StringRepresentable.fromEnum(SolarTier::values);

    private final String name;
    /** 1-based tier number. */
    public final int tier;
    /** Footprint edge length in blocks: T1 = 1 (1×1), T2 = 2, T3 = 3 (parity; placement is 1×1 for now). */
    public final int footprint;
    private final int baseFePerTick;
    private final int baseBuffer;

    SolarTier(String name, int tier, int footprint, int baseFePerTick, int baseBuffer) {
        this.name = name;
        this.tier = tier;
        this.footprint = footprint;
        this.baseFePerTick = baseFePerTick;
        this.baseBuffer = baseBuffer;
    }

    /** Peak FE/tick this single panel adds at full sun (config-scaled). */
    public int fePerTick() {
        return NerospaceConfig.scale(this.baseFePerTick, NerospaceConfig.energyRateMultiplier());
    }

    /** This panel's own FE buffer; an array's total storage is the sum across its members. */
    public int buffer() {
        return NerospaceConfig.scale(this.baseBuffer, NerospaceConfig.energyRateMultiplier());
    }

    /** Per-tier extract throughput (well above the peak output, so a pipe never bottlenecks the array). */
    public int maxExtract() {
        return Math.max(256, fePerTick() * 16);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
