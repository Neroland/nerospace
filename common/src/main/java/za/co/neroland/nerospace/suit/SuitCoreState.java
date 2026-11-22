package za.co.neroland.nerospace.suit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** The chest piece's {@code nerospace:suit_core}: energy (FE), RCS propellant (mB) and jet heat. */
public record SuitCoreState(int energy, int rcs, int heat) {

    public static final SuitCoreState EMPTY = new SuitCoreState(0, 0, 0);
    /** RCS reservoir filled by Milestone B's EVA thrusters. Stored now so the field is not new later. */
    public static final int RCS_CAPACITY = 2_000;

    public static final Codec<SuitCoreState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("energy", 0).forGetter(SuitCoreState::energy),
            Codec.INT.optionalFieldOf("rcs", 0).forGetter(SuitCoreState::rcs),
            Codec.INT.optionalFieldOf("heat", 0).forGetter(SuitCoreState::heat)
    ).apply(instance, SuitCoreState::normalized));

    public static SuitCoreState normalized(Integer energy, Integer rcs, Integer heat) {
        return new SuitCoreState(clamp(energy, 0, Integer.MAX_VALUE), clamp(rcs, 0, RCS_CAPACITY),
                clamp(heat, 0, 255));
    }

    private static int clamp(Integer value, int min, int max) {
        int v = value == null ? min : value;
        return Math.max(min, Math.min(max, v));
    }

    public SuitCoreState withEnergy(int next) {
        return normalized(next, this.rcs, this.heat);
    }

    public SuitCoreState withHeat(int next) {
        return normalized(this.energy, this.rcs, next);
    }
}
