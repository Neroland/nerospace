package za.co.neroland.nerospace.block.rules;

import java.util.List;

/**
 * Airtight hull (002 §1.7, §10.5; 005 §7.1): {@code nerospace:airtight} ⊂ {@code nerospace:oxygen_sealing}; only
 * Hull Plating and Hull Window are in it. Station Wall/Floor stay sealing-only.
 */
public final class AirtightRules {

    /** The locked membership of {@code nerospace:airtight}. */
    public static final List<String> AIRTIGHT = List.of("nerospace:hull_plating", "nerospace:hull_window");

    private AirtightRules() {
    }

    public static boolean isAirtight(boolean inAirtightTag) {
        return inAirtightTag;
    }

    /** A hull cell holds pressure if it is airtight or (legacy) oxygen-sealing. */
    public static boolean seals(boolean airtight, boolean oxygenSealing) {
        return airtight || oxygenSealing;
    }

    /**
     * Console "pressurised": a module counts when its source cell reads max and every hull cell seals.
     * Returns the number of such modules.
     */
    public static int countSealed(boolean[] sourceAtMax, boolean[] allHullSealed) {
        int n = 0;
        for (int i = 0; i < Math.min(sourceAtMax.length, allHullSealed.length); i++) {
            if (sourceAtMax[i] && allHullSealed[i]) {
                n++;
            }
        }
        return n;
    }

    /** Airtight-counted hull blocks on a module (Hull Plating/Window only). */
    public static int airtightCount(boolean[] hullCellIsAirtight) {
        int n = 0;
        for (boolean b : hullCellIsAirtight) {
            if (b) {
                n++;
            }
        }
        return n;
    }
}
