package za.co.neroland.nerospace.world.station;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Module material costs, as item id → count. Constants, not config. The console consumes the
 * exact set from the builder's inventory on a Build click, atomically, before a single block is placed. Repairs
 * are free; the two core kinds cost nothing.
 */
public final class ModuleCosts {

    private ModuleCosts() {
    }

    /** The cost of one module of {@code kind} (an ordered, unmodifiable map; empty for the core kinds). */
    public static Map<String, Integer> of(ModuleKind kind) {
        Map<String, Integer> m = new LinkedHashMap<>();
        switch (kind) {
            case CORE_LEGACY, CORE -> {
                // Free: the charter paid for the core.
            }
            case DOCKING_RING -> {
                m.put("nerospace:hull_plating", 24);
                m.put("nerospace:titanium_ingot", 12);
                m.put("nerospace:nerosteel_ingot", 4);
            }
            case HABITAT_RING -> {
                m.put("nerospace:hull_plating", 64);
                m.put("nerospace:hull_window", 24);
                m.put("nerospace:station_floor", 32);
                m.put("nerospace:titanium_ingot", 8);
            }
            case SOLAR_WING -> {
                m.put("nerospace:truss", 8);
                m.put("nerospace:solar_wing", 12);
            }
            case HYDROPONICS_BAY -> {
                m.put("nerospace:hull_plating", 32);
                m.put("nerospace:hull_window", 16);
                m.put("nerospace:hydroponics_tray", 12);
                m.put("nerospace:station_floor", 8);
            }
            case GRAVITY_DECK -> {
                m.put("nerospace:hull_plating", 24);
                m.put("nerospace:hull_window", 8);
                m.put("nerospace:gravity_plate", 9);
                m.put("nerospace:gravity_shard", 4);
            }
            case OBSERVATORY -> {
                m.put("nerospace:hull_window", 16);
                m.put("nerospace:hull_plating", 8);
                m.put("nerospace:truss", 4);
                m.put("nerospace:titanium_ingot", 8);
                m.put("nerospace:voidglass_lens", 1);
                m.put("nerospace:void_crystal", 2);
            }
        }
        return Collections.unmodifiableMap(m);
    }

    /** What is still missing given what the player holds (id → count); empty means the cost is satisfiable. */
    public static Map<String, Integer> shortfall(ModuleKind kind, Map<String, Integer> have) {
        Map<String, Integer> missing = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : of(kind).entrySet()) {
            int held = have.getOrDefault(e.getKey(), 0);
            if (held < e.getValue()) {
                missing.put(e.getKey(), e.getValue() - held);
            }
        }
        return missing;
    }

    public static boolean satisfiable(ModuleKind kind, Map<String, Integer> have) {
        return shortfall(kind, have).isEmpty();
    }
}
