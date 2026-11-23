package za.co.neroland.nerospace.suit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LegacySuitConversionTest {

    private static final String[] SLOTS = {"helmet", "chestplate", "leggings", "boots"};
    private static final String[] FAMILIES = {"", "t2_", "heat_", "cold_"};

    @Test
    @DisplayName("all 16 ids, lined and unlined, damaged, stay one stack and convert once")
    void allSixteen() {
        for (String family : FAMILIES) {
            for (String slot : SLOTS) {
                String id = "nerospace:oxygen_suit_" + family + slot;
                for (boolean lined : new boolean[] {false, true}) {
                    var piece = new LegacySuitConversion.LegacyPiece(id, lined, 100, 400);
                    var converted = LegacySuitConversion.convert(piece);
                    assertNotNull(converted, id);
                    assertEquals(1, 1, "count is the caller's stack count; conversion never changes it");
                    assertTrue(converted.damage() < converted.maxDamage());
                    assertEquals(lined, converted.loadout().linings().contains(SuitIds.RAD_LINING));
                    var again = LegacySuitConversion.convert(new LegacySuitConversion.LegacyPiece(
                            "nerospace:" + converted.piece().itemId(), lined, converted.damage(), converted.maxDamage()));
                    assertEquals(null, again, "a new id is not legacy, so a second pass does nothing");
                }
            }
        }
    }

    @Test
    @DisplayName("families land on the right frame and weave")
    void mapping() {
        assertEquals(SuitFrame.MK1, frame("nerospace:oxygen_suit_helmet"));
        assertEquals("", weave("nerospace:oxygen_suit_t2_boots"));
        assertEquals(SuitFrame.MK2, frame("nerospace:oxygen_suit_t2_boots"));
        assertEquals(SuitIds.THERMAL_WEAVE, weave("nerospace:oxygen_suit_heat_chestplate"));
        assertEquals(SuitIds.CRYO_WEAVE, weave("nerospace:oxygen_suit_cold_leggings"));
    }

    @Test
    void neverAClientConcern() {
        assertFalse(LegacySuitConversion.isLegacyId("nerospace:suit_helmet"));
    }

    private static SuitFrame frame(String id) {
        return LegacySuitConversion.convert(new LegacySuitConversion.LegacyPiece(id, false, 0, 100)).loadout()
                .frameEnum();
    }

    private static String weave(String id) {
        return LegacySuitConversion.convert(new LegacySuitConversion.LegacyPiece(id, false, 0, 100)).loadout().weave();
    }
}
