package za.co.neroland.nerospace.suit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.world.planet.HazardOrderRules;

/**
 * The release gate: every old full set, mixed set, hazard, frigid flag and Dawnband shelter resolves the same
 * way for the legacy classification, the suit view of those pieces, and the converted suit.
 */
class HazardParityTest {

    private static final String[] FAMILIES = {"", "t2_", "heat_", "cold_"};

    @Test
    @DisplayName("old class, viewed class and converted class agree, and the hazard does too")
    void parity() {
        for (String head : families()) {
            for (String chest : families()) {
                for (String legs : families()) {
                    for (String feet : families()) {
                        for (boolean lined : new boolean[] {false, true}) {
                            compare(head, chest, legs, feet, lined);
                        }
                    }
                }
            }
        }
    }

    private static void compare(String head, String chest, String legs, String feet, boolean lined) {
        String h = id(head, "helmet");
        String c = id(chest, "chestplate");
        String l = id(legs, "leggings");
        String f = id(feet, "boots");
        HazardOrderRules.SuitClass legacy = LegacySuitConversion.legacyClass(h, c, l, f);
        boolean full = legacy != HazardOrderRules.SuitClass.NONE;
        boolean rad = LegacySuitConversion.legacyRadLined(lined, lined, lined, lined, full);
        SuitProtection.Reading viewed = SuitProtection.read(SuitProtection.Slot.legacy(h, lined, false),
                SuitProtection.Slot.legacy(c, lined, false), SuitProtection.Slot.legacy(l, lined, false),
                SuitProtection.Slot.legacy(f, lined, false));
        SuitProtection.Reading converted = SuitProtection.read(modern(h, lined), modern(c, lined), modern(l, lined),
                modern(f, lined));
        assertEquals(legacy, viewed.suit(), h + " " + c);
        assertEquals(legacy, converted.suit(), h + " " + c);
        assertEquals(rad, viewed.radLined());
        assertEquals(rad, converted.radLined());
        for (boolean frigid : new boolean[] {false, true}) {
            for (boolean dawn : new boolean[] {false, true}) {
                for (Hazard present : List.of(Hazard.NONE, Hazard.HEAT, Hazard.COLD, Hazard.RADIATION)) {
                    var set = present == Hazard.NONE ? java.util.EnumSet.noneOf(Hazard.class)
                            : java.util.EnumSet.of(present);
                    Hazard expected = HazardOrderRules.resolve(set, legacy, rad, frigid, dawn);
                    assertEquals(expected, HazardOrderRules.resolve(set, viewed.suit(), viewed.radLined(), frigid, dawn));
                    assertEquals(expected,
                            HazardOrderRules.resolve(set, converted.suit(), converted.radLined(), frigid, dawn));
                }
            }
        }
    }

    private static SuitProtection.Slot modern(String legacyId, boolean lined) {
        var converted = LegacySuitConversion.convert(new LegacySuitConversion.LegacyPiece(legacyId, lined, 0, 100));
        return SuitProtection.Slot.modern(converted.loadout(), false, lined);
    }

    private static List<String> families() {
        return List.of(FAMILIES);
    }

    private static String id(String family, String slot) {
        return "nerospace:oxygen_suit_" + family + slot;
    }
}
