package za.co.neroland.nerospace.suit;

import za.co.neroland.nerospace.world.planet.HazardOrderRules;

/**
 * The mapping from the sixteen legacy suit ids onto a Pathfinder piece. Pure: no item stacks, so it runs in
 * unit tests and on the server the same way. Conversion is idempotent — a new-suit id is not a legacy id.
 */
public final class LegacySuitConversion {

    public enum Family {
        T1, T2, HEAT, COLD
    }

    public record LegacyPiece(String itemId, boolean radLined, int damage, int maxDamage) {
    }

    public record Converted(SuitPiece piece, SuitLoadout loadout, int damage, int maxDamage) {
    }

    private LegacySuitConversion() {
    }

    public static boolean isLegacyId(String itemId) {
        return family(itemId) != null && piece(itemId) != null;
    }

    public static Family family(String itemId) {
        if (itemId == null || !itemId.startsWith("nerospace:oxygen_suit_")) {
            return null;
        }
        String rest = itemId.substring("nerospace:oxygen_suit_".length());
        if (rest.startsWith("t2_")) {
            return Family.T2;
        }
        if (rest.startsWith("heat_")) {
            return Family.HEAT;
        }
        if (rest.startsWith("cold_")) {
            return Family.COLD;
        }
        return Family.T1;
    }

    public static SuitPiece piece(String itemId) {
        if (itemId == null || !itemId.startsWith("nerospace:oxygen_suit")) {
            return null;
        }
        int slash = itemId.lastIndexOf('_');
        if (slash < 0) {
            return null;
        }
        return SuitPiece.bySlotName(itemId.substring(slash + 1));
    }

    /** One legacy stack → the new loadout. Enchantments, name and trim are carried by the caller. */
    public static Converted convert(LegacyPiece piece) {
        Family family = family(piece.itemId());
        SuitPiece slot = piece(piece.itemId());
        if (family == null || slot == null) {
            return null;
        }
        SuitFrame frame = family == Family.T1 ? SuitFrame.MK1 : SuitFrame.MK2;
        String weave = switch (family) {
            case HEAT -> SuitIds.THERMAL_WEAVE;
            case COLD -> SuitIds.CRYO_WEAVE;
            default -> "";
        };
        var linings = piece.radLined() ? java.util.List.of(SuitIds.RAD_LINING) : java.util.List.<String>of();
        SuitLoadout loadout = new SuitLoadout(frame.ordinal(), weave, linings, java.util.List.of(), 0, -1, 0);
        int oldMax = Math.max(1, piece.maxDamage());
        int newMax = SuitDurability.max(slot, frame);
        int newDamage = (int) Math.round(Math.max(0, piece.damage()) * (newMax / (double) oldMax));
        newDamage = Math.min(newDamage, newMax - 1);
        return new Converted(slot, loadout, newDamage, newMax);
    }

    /**
     * The suit class today's {@code OxygenManager.suitClass} would return for these four item ids, copied here
     * so the parity test does not depend on the live player path. A null id is an empty slot.
     */
    public static HazardOrderRules.SuitClass legacyClass(String head, String chest, String legs, String feet) {
        if (!full(head, chest, legs, feet)) {
            return HazardOrderRules.SuitClass.NONE;
        }
        if (allFamily(Family.HEAT, head, chest, legs, feet)) {
            return HazardOrderRules.SuitClass.THERMAL;
        }
        if (allFamily(Family.COLD, head, chest, legs, feet)) {
            return HazardOrderRules.SuitClass.CRYO;
        }
        if (tier2Class(head) && tier2Class(chest) && tier2Class(legs) && tier2Class(feet)) {
            return HazardOrderRules.SuitClass.T2;
        }
        return HazardOrderRules.SuitClass.T1;
    }

    public static boolean legacyRadLined(boolean head, boolean chest, boolean legs, boolean feet, boolean fullSuit) {
        return fullSuit && head && chest && legs && feet;
    }

    private static boolean full(String... ids) {
        for (String id : ids) {
            if (piece(id) == null) {
                return false;
            }
        }
        return piece(ids[0]) == SuitPiece.HELMET && piece(ids[1]) == SuitPiece.CHEST
                && piece(ids[2]) == SuitPiece.LEGS && piece(ids[3]) == SuitPiece.BOOTS;
    }

    private static boolean allFamily(Family family, String... ids) {
        for (String id : ids) {
            if (family(id) != family) {
                return false;
            }
        }
        return true;
    }

    private static boolean tier2Class(String id) {
        Family family = family(id);
        return family == Family.T2 || family == Family.HEAT || family == Family.COLD;
    }
}
