package za.co.neroland.nerospace.suit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SuitSlotRulesTest {

    @Test
    @DisplayName("frame, piece and kind gates, stacking caps, and the jump-jet / harness clash")
    void gates() {
        SuitLoadout mk1 = SuitLoadout.EMPTY;
        assertTrue(SuitSlotRules.accepts(mk1, SuitPiece.CHEST, SuitIds.LIFE_SUPPORT_TANK));
        assertFalse(SuitSlotRules.accepts(mk1, SuitPiece.CHEST, SuitIds.THERMAL_WEAVE));
        assertFalse(SuitSlotRules.accepts(mk1, SuitPiece.HELMET, SuitIds.LIFE_SUPPORT_TANK));
        SuitLoadout mk2 = mk1.withFrame(SuitFrame.MK2);
        assertTrue(SuitSlotRules.accepts(mk2, SuitPiece.HELMET, SuitIds.THERMAL_WEAVE));
        SuitLoadout woven = mk2.withWeave(SuitIds.THERMAL_WEAVE);
        assertFalse(SuitSlotRules.accepts(woven, SuitPiece.HELMET, SuitIds.CRYO_WEAVE));
        SuitLoadout tanks = mk2.withModules(java.util.List.of(SuitIds.LIFE_SUPPORT_TANK, SuitIds.LIFE_SUPPORT_TANK));
        assertFalse(SuitSlotRules.accepts(tanks, SuitPiece.CHEST, SuitIds.LIFE_SUPPORT_TANK));
        SuitLoadout jet = mk2.withFrame(SuitFrame.MK3).withModules(java.util.List.of(SuitIds.JUMP_JET));
        assertFalse(SuitSlotRules.accepts(jet, SuitPiece.CHEST, SuitIds.FLIGHT_HARNESS));
        assertFalse(SuitSlotRules.accepts(mk2, SuitPiece.CHEST, SuitIds.FLIGHT_HARNESS), "harness needs Mk IV");
    }

    @Test
    @DisplayName("a broken data file falls back and logs once")
    void fallback() {
        SuitSlotRules.resetFallbackLog();
        assertFalse(SuitSlotRules.overlay("{not json"));
        assertFalse(SuitSlotRules.overlay("{not json"));
        assertTrue(SuitSlotRules.accepts(SuitLoadout.EMPTY, SuitPiece.CHEST, SuitIds.LIFE_SUPPORT_TANK));
    }
}
