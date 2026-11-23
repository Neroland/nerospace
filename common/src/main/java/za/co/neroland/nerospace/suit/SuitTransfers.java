package za.co.neroland.nerospace.suit;

import java.util.ArrayList;
import java.util.List;

/** Install and remove modules. The returned counts are what the slot is left holding — nothing is created. */
public final class SuitTransfers {

    public record Move(SuitLoadout loadout, int cursorCount, boolean applied) {
    }

    private SuitTransfers() {
    }

    public static Move install(SuitLoadout loadout, SuitPiece piece, String id, int cursorCount) {
        if (cursorCount <= 0 || !SuitSlotRules.accepts(loadout, piece, id)) {
            return new Move(loadout, cursorCount, false);
        }
        SuitSlotRules.Rule rule = SuitSlotRules.rule(id, piece);
        SuitLoadout next;
        if (rule.kind() == SuitSlotKind.WEAVE) {
            next = loadout.withWeave(id);
        } else if (rule.kind() == SuitSlotKind.LINING) {
            List<String> linings = new ArrayList<>(loadout.linings());
            linings.add(id);
            next = loadout.withLinings(linings);
        } else {
            List<String> modules = new ArrayList<>(loadout.modules());
            modules.add(id);
            next = loadout.withModules(modules);
        }
        return new Move(next, cursorCount - 1, true);
    }

    public static Move remove(SuitLoadout loadout, SuitSlotKind kind, int index) {
        if (kind == SuitSlotKind.WEAVE) {
            if (!loadout.hasWeave()) {
                return new Move(loadout, 0, false);
            }
            return new Move(loadout.withWeave(""), 1, true);
        }
        List<String> source = kind == SuitSlotKind.LINING ? loadout.linings() : loadout.modules();
        if (index < 0 || index >= source.size()) {
            return new Move(loadout, 0, false);
        }
        List<String> next = new ArrayList<>(source);
        next.remove(index);
        SuitLoadout updated = kind == SuitSlotKind.LINING ? loadout.withLinings(next) : loadout.withModules(next);
        return new Move(updated, 1, true);
    }
}
