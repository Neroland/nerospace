package za.co.neroland.nerospace;

import java.util.Set;

/**
 * The ores and raw materials that moved to Neroland Core 1.25.0. Their blockstates, models, textures and loot tables
 * now ship in Core under {@code nerolandcore:}; Core's jar for the same cell is on the test classpath, so the asset
 * and loot checks look them up there instead of in Nerospace's own resources.
 */
public final class MovedOres {

    public static final Set<String> BLOCKS = Set.of("nerosium_ore", "deepslate_nerosium_ore", "raw_nerosium_block",
            "nerosteel_ore", "xertz_quartz_ore", "amber_sap_ore", "ilmenite_ore", "cheese_ore", "hematite_ore",
            "cindrite_ore", "glacite_ore", "voidglass_ore", "brine_salt_ore", "vent_sulfide_ore", "europium_ore");

    public static final Set<String> ITEMS = Set.of("raw_nerosium", "raw_nerosteel", "xertz_quartz", "amber_sap",
            "raw_ilmenite", "moon_cheese", "raw_hematite", "cindrite", "glacite", "voidglass", "brine_salt",
            "raw_vent_sulfide", "raw_europium");

    private MovedOres() {
    }

    public static boolean moved(String id) {
        return BLOCKS.contains(id) || ITEMS.contains(id);
    }

    /** The namespace that owns this id's assets and loot: {@code nerolandcore} for a moved ore, else {@code nerospace}. */
    public static String ns(String id) {
        return moved(id) ? "nerolandcore" : "nerospace";
    }
}
