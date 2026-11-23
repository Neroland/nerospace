package za.co.neroland.nerospace.suit;

/**
 * Pathfinder Suit frames. Append-only: Milestone B adds {@code MK5} at the end. An unknown ordinal read from a
 * newer save resolves to {@link #MK1} (decision 001 rule 3).
 */
public enum SuitFrame {
    /** Pressure shell. Today's Tier 1 armour and a 50 000 FE battery. */
    MK1(50_000, 0, 0, 1, 0, 0.0F),
    /** Cindrite frame. Today's Tier 2 armour, one weave, one lining, 100 000 FE. */
    MK2(100_000, 1, 1, 2, 0, 0.0F),
    /** Titanium frame. Tier 2 armour, +1 toughness, two linings, chest has three module slots, 250 000 FE. */
    MK3(250_000, 1, 2, 2, 1, 0.0F),
    /** Voidglass frame. Tier 2 armour, +2 toughness, +0.1 knockback resistance, three module slots, 500 000 FE. */
    MK4(500_000, 1, 2, 3, 2, 0.1F);

    /** Battery size in FE before Power Cells and the capacity multiplier. */
    public final int battery;
    public final int weaveSlots;
    public final int liningSlots;
    /** Module slots on every piece except the Mk III chest, which has one extra. */
    public final int moduleSlots;
    /** Added to the Tier 2 toughness (Mk I uses the Tier 1 toughness and ignores this). */
    public final int toughnessBonus;
    public final float knockbackResistance;

    SuitFrame(int battery, int weaveSlots, int liningSlots, int moduleSlots, int toughnessBonus,
            float knockbackResistance) {
        this.battery = battery;
        this.weaveSlots = weaveSlots;
        this.liningSlots = liningSlots;
        this.moduleSlots = moduleSlots;
        this.toughnessBonus = toughnessBonus;
        this.knockbackResistance = knockbackResistance;
    }

    /** Unknown or out-of-range ordinals are Mk I. */
    public static SuitFrame byOrdinal(int ordinal) {
        SuitFrame[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return MK1;
        }
        return values[ordinal];
    }

    public int moduleSlots(SuitPiece piece) {
        if (this == MK3 && piece == SuitPiece.CHEST) {
            return 3;
        }
        return this.moduleSlots;
    }

    /** Armour points for this piece: Mk I is today's Tier 1, every later frame is today's Tier 2. */
    public int armor(SuitPiece piece) {
        if (this == MK1) {
            return switch (piece) {
                case HELMET, BOOTS -> 3;
                case CHEST -> 7;
                case LEGS -> 6;
            };
        }
        return switch (piece) {
            case HELMET, BOOTS -> 4;
            case CHEST -> 8;
            case LEGS -> 6;
        };
    }

    /** Toughness. Mk I is 1.5; later frames start at 2 and add {@link #toughnessBonus}. */
    public float toughness() {
        return this == MK1 ? 1.5F : 2.0F + this.toughnessBonus;
    }

    /** Vanilla durability multiplier: 28 for Mk I, 36 afterwards (the old T1 / T2 materials). */
    public int durabilityMultiplier() {
        return this == MK1 ? 28 : 36;
    }
}
