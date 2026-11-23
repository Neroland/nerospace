package za.co.neroland.nerospace.suit;

import za.co.neroland.nerospace.world.planet.HazardOrderRules;

/**
 * Turns four worn pieces into the suit class and radiation flag {@link HazardOrderRules} already understands.
 * A breached piece counts as missing. A weave or lining counts only when all four pieces carry it.
 */
public final class SuitProtection {

    /** One worn slot. {@code legacyId} is set for an unconverted piece; {@code loadout} for a new piece. */
    public record Slot(boolean suit, boolean breached, String legacyId, SuitLoadout loadout, boolean radComponent) {

        public static Slot empty() {
            return new Slot(false, false, null, null, false);
        }

        public static Slot legacy(String itemId, boolean radLined, boolean breached) {
            return new Slot(LegacySuitConversion.piece(itemId) != null, breached, itemId, null, radLined);
        }

        public static Slot modern(SuitLoadout loadout, boolean breached, boolean radComponent) {
            return new Slot(true, breached, null, loadout == null ? SuitLoadout.EMPTY : loadout, radComponent);
        }
    }

    public record Reading(HazardOrderRules.SuitClass suit, boolean radLined, SuitFrame effective) {
        public static final Reading NONE = new Reading(HazardOrderRules.SuitClass.NONE, false, null);
    }

    private SuitProtection() {
    }

    public static Reading read(Slot head, Slot chest, Slot legs, Slot feet) {
        if (!sealed(head) || !sealed(chest) || !sealed(legs) || !sealed(feet)) {
            return Reading.NONE;
        }
        if (!positions(head, chest, legs, feet)) {
            return Reading.NONE;
        }
        SuitFrame effective = min(frame(head), frame(chest), frame(legs), frame(feet));
        String weave = sharedWeave(head, chest, legs, feet);
        boolean rad = sharedLining(head, chest, legs, feet, SuitIds.RAD_LINING) || allComponents(head, chest, legs, feet);
        HazardOrderRules.SuitClass suit;
        if (SuitIds.ADAPTIVE_WEAVE.equals(weave)) {
            suit = HazardOrderRules.SuitClass.ADAPTIVE;
        } else if (SuitIds.THERMAL_WEAVE.equals(weave)) {
            suit = HazardOrderRules.SuitClass.THERMAL;
        } else if (SuitIds.CRYO_WEAVE.equals(weave)) {
            suit = HazardOrderRules.SuitClass.CRYO;
        } else if (effective.ordinal() >= SuitFrame.MK2.ordinal()) {
            suit = HazardOrderRules.SuitClass.T2;
        } else {
            suit = HazardOrderRules.SuitClass.T1;
        }
        return new Reading(suit, rad, effective);
    }

    private static boolean sealed(Slot slot) {
        return slot != null && slot.suit() && !slot.breached();
    }

    /** Legacy pieces must sit in the matching slot. A new piece is already the item for that argument. */
    private static boolean positions(Slot head, Slot chest, Slot legs, Slot feet) {
        return position(head, SuitPiece.HELMET) && position(chest, SuitPiece.CHEST)
                && position(legs, SuitPiece.LEGS) && position(feet, SuitPiece.BOOTS);
    }

    private static boolean position(Slot slot, SuitPiece expected) {
        if (slot.legacyId() != null) {
            return LegacySuitConversion.piece(slot.legacyId()) == expected;
        }
        return true;
    }

    private static SuitFrame frame(Slot slot) {
        if (slot.loadout() != null && slot.legacyId() == null) {
            return slot.loadout().frameEnum();
        }
        LegacySuitConversion.Family family = LegacySuitConversion.family(slot.legacyId());
        if (family == null || family == LegacySuitConversion.Family.T1) {
            return SuitFrame.MK1;
        }
        return SuitFrame.MK2;
    }

    private static String weaveOf(Slot slot) {
        if (slot.loadout() != null && slot.legacyId() == null) {
            return slot.loadout().weave();
        }
        LegacySuitConversion.Family family = LegacySuitConversion.family(slot.legacyId());
        if (family == LegacySuitConversion.Family.HEAT) {
            return SuitIds.THERMAL_WEAVE;
        }
        if (family == LegacySuitConversion.Family.COLD) {
            return SuitIds.CRYO_WEAVE;
        }
        return "";
    }

    private static boolean lining(Slot slot, String id) {
        if (slot.loadout() != null && slot.legacyId() == null) {
            return slot.loadout().linings().contains(id) || (SuitIds.RAD_LINING.equals(id) && slot.radComponent());
        }
        return SuitIds.RAD_LINING.equals(id) && slot.radComponent();
    }

    private static String sharedWeave(Slot... slots) {
        String weave = weaveOf(slots[0]);
        if (weave == null || weave.isEmpty()) {
            return "";
        }
        for (Slot slot : slots) {
            if (!weave.equals(weaveOf(slot))) {
                return "";
            }
        }
        return weave;
    }

    private static boolean sharedLining(Slot head, Slot chest, Slot legs, Slot feet, String id) {
        return lining(head, id) && lining(chest, id) && lining(legs, id) && lining(feet, id);
    }

    private static boolean allComponents(Slot head, Slot chest, Slot legs, Slot feet) {
        return head.radComponent() && chest.radComponent() && legs.radComponent() && feet.radComponent();
    }

    private static SuitFrame min(SuitFrame... frames) {
        SuitFrame best = frames[0];
        for (SuitFrame frame : frames) {
            if (frame.ordinal() < best.ordinal()) {
                best = frame;
            }
        }
        return best;
    }
}
