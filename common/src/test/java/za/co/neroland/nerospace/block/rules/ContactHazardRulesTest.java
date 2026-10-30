package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Perchlorate poison, Ember heat, moss nausea — three gates, three effects. */
@SuppressWarnings("unused")
class ContactHazardRulesTest {

    @Test
    @DisplayName("poison: living without sealed boots, Poison I 4 s")
    void poison() {
        assertTrue(ContactHazardRules.poison(true, false));
        assertFalse(ContactHazardRules.poison(true, true));
        assertFalse(ContactHazardRules.poison(false, false));
        assertEquals(80, ContactHazardRules.POISON_TICKS);
        assertEquals(0, ContactHazardRules.POISON_AMPLIFIER);
    }

    @Test
    @DisplayName("heat: 1.0 damage every 20 ticks without heat boots")
    void heat() {
        assertEquals(1.0F, HeatContactRules.damage(true, false));
        assertEquals(0.0F, HeatContactRules.damage(true, true));
        assertEquals(0.0F, HeatContactRules.damage(false, false));
        assertTrue(HeatContactRules.dueThisTick(40));
        assertFalse(HeatContactRules.dueThisTick(41));
        assertTrue(HeatContactRules.dueThisTick(-20));
    }

    @Test
    @DisplayName("moss: point Nausea I 5 s without a sealed helmet")
    void moss() {
        assertTrue(SporeContactRules.nausea(false));
        assertFalse(SporeContactRules.nausea(true));
        assertEquals(100, SporeContactRules.MOSS_NAUSEA_TICKS);
    }
}
