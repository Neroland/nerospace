package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.station.ModuleCosts;
import za.co.neroland.nerospace.world.station.ModuleKind;

/** The exact module costs, and that a shortfall of one item refuses the build. */
class ModuleCostsTest {

    @Test
    void exactMultisets() {
        assertEquals(Map.of("nerospace:hull_plating", 24, "nerospace:titanium_ingot", 12,
                "nerospace:nerosteel_ingot", 4), ModuleCosts.of(ModuleKind.DOCKING_RING));
        assertEquals(Map.of("nerospace:hull_plating", 64, "nerospace:hull_window", 24, "nerospace:station_floor", 32,
                "nerospace:titanium_ingot", 8), ModuleCosts.of(ModuleKind.HABITAT_RING));
        assertEquals(Map.of("nerospace:truss", 8, "nerospace:solar_wing", 12), ModuleCosts.of(ModuleKind.SOLAR_WING));
        assertEquals(Map.of("nerospace:hull_plating", 32, "nerospace:hull_window", 16,
                "nerospace:hydroponics_tray", 12, "nerospace:station_floor", 8),
                ModuleCosts.of(ModuleKind.HYDROPONICS_BAY));
        assertEquals(Map.of("nerospace:hull_plating", 24, "nerospace:hull_window", 8, "nerospace:gravity_plate", 9,
                "nerospace:gravity_shard", 4), ModuleCosts.of(ModuleKind.GRAVITY_DECK));
        assertEquals(Map.of("nerospace:hull_window", 16, "nerospace:hull_plating", 8, "nerospace:truss", 4,
                "nerospace:titanium_ingot", 8, "nerospace:voidglass_lens", 1, "nerospace:void_crystal", 2),
                ModuleCosts.of(ModuleKind.OBSERVATORY));
        assertTrue(ModuleCosts.of(ModuleKind.CORE).isEmpty());
    }

    @Test
    void oneShortIsNotSatisfiable() {
        Map<String, Integer> have = new HashMap<>(ModuleCosts.of(ModuleKind.SOLAR_WING));
        assertTrue(ModuleCosts.satisfiable(ModuleKind.SOLAR_WING, have));
        have.put("nerospace:solar_wing", 11);
        assertFalse(ModuleCosts.satisfiable(ModuleKind.SOLAR_WING, have));
        assertEquals(Map.of("nerospace:solar_wing", 1), ModuleCosts.shortfall(ModuleKind.SOLAR_WING, have));
    }
}
