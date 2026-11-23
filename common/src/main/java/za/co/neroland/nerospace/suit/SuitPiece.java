package za.co.neroland.nerospace.suit;

/** The four vanilla armour slots the suit uses. */
public enum SuitPiece {
    HELMET("helmet", "helmet", 11),
    CHEST("chestplate", "chestplate", 16),
    LEGS("leggings", "leggings", 15),
    BOOTS("boots", "boots", 13);

    /** Item path suffix: {@code suit_<id>}. */
    public final String id;
    /** Legacy item suffix, the same four names. */
    public final String legacySuffix;
    /** Vanilla armour durability factor (helmet 11, chest 16, legs 15, boots 13). */
    public final int durabilityFactor;

    SuitPiece(String id, String legacySuffix, int durabilityFactor) {
        this.id = id;
        this.legacySuffix = legacySuffix;
        this.durabilityFactor = durabilityFactor;
    }

    public String itemId() {
        return "suit_" + this.id;
    }

    public static SuitPiece bySlotName(String suffix) {
        for (SuitPiece piece : values()) {
            if (piece.legacySuffix.equals(suffix) || piece.id.equals(suffix)) {
                return piece;
            }
        }
        return null;
    }
}
