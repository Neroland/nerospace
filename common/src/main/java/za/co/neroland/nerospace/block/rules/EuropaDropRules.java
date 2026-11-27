package za.co.neroland.nerospace.block.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * The drop-list changes of Europa's four conditional ores, applied to what the loot table produced. Kept free of game
 * types (a stack is an item id and a count) so the list transformation is tested without a level. The block classes
 * call this from their {@code getDrops} override, which is the one path players, explosions and the quarry all take.
 *
 * <p>Silk Touch always leaves the table's result alone. An empty table result stays empty (wrong tool, or an
 * explosion that destroyed the drop).</p>
 */
public final class EuropaDropRules {

    public static final String BRINE_SALT = "nerolandcore:brine_salt";
    public static final String OXIDANT_CRYSTAL = "nerospace:oxidant_crystal";
    public static final String SULFUR = "nerospace:sulfur";

    /** The four blocks with a conditional drop, and what {@code flag} means for each. */
    public enum Kind {
        /** flag = a heat source within 4 blocks. */
        DEUTERIUM_ICE,
        /** flag = brine in any of the 6 neighbours. */
        BRINE_SALT_ORE,
        /** flag = the ore is wet. */
        VENT_SULFIDE_ORE,
        /** flag unused; {@code charge} is the block's charge. */
        OXIDANT_ICE
    }

    /** An item id and a count. */
    public record Stack(String id, int count) {
    }

    private EuropaDropRules() {
    }

    public static List<Stack> apply(Kind kind, List<Stack> base, boolean silk, boolean flag, int charge) {
        if (silk || base.isEmpty()) {
            return base;
        }
        switch (kind) {
            case DEUTERIUM_ICE:
                return SublimeRules.yields(flag) ? base : List.of();
            case BRINE_SALT_ORE: {
                int multiplier = WetSeamRules.multiplier(flag);
                if (multiplier == 1) {
                    return base;
                }
                List<Stack> out = new ArrayList<>(base.size());
                for (Stack s : base) {
                    out.add(BRINE_SALT.equals(s.id()) ? new Stack(s.id(), WetSeamRules.count(s.count(), multiplier)) : s);
                }
                return out;
            }
            case VENT_SULFIDE_ORE:
                return SulfideRules.dropsOre(flag) ? base : List.of(new Stack(SULFUR, 1));
            case OXIDANT_ICE: {
                List<Stack> out = new ArrayList<>(base.size());
                for (Stack s : base) {
                    out.add(OXIDANT_CRYSTAL.equals(s.id()) ? new Stack(s.id(), RadiolysisRules.yield(charge)) : s);
                }
                return out;
            }
            default:
                return base;
        }
    }
}
