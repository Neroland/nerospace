package za.co.neroland.nerospace.world.planet.cindara;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.block.rules.LightningRules;
import za.co.neroland.nerospace.config.NerospaceConfig;

/** Ember strikes: strong Ash Fall only, one step per interval, one strike per player area, ≤ 4 per level-second. */
class EmberStrikeTest {

    @Test
    void onlyStrongAshFallStrikes() {
        assertTrue(EmberStrike.stepDue(true, 0.7, 0, EmberStrike.NEVER, 100));
        assertTrue(EmberStrike.stepDue(true, 1.0, 500, 400, 100));
        assertFalse(EmberStrike.stepDue(true, 0.69, 0, EmberStrike.NEVER, 100), "below the ember-storm intensity");
        assertFalse(EmberStrike.stepDue(false, 1.0, 0, EmberStrike.NEVER, 100), "no ash fall, no strikes");
    }

    @Test
    void atMostOneStepPerInterval() {
        for (int interval : new int[] {40, 50, 100, 130}) {
            long last = EmberStrike.NEVER;
            long previous = Long.MIN_VALUE;
            int steps = 0;
            // onSecond fires every 20 ticks
            for (long t = 0; t < 10_000; t += 20) {
                if (EmberStrike.stepDue(true, 1.0, t, last, interval)) {
                    if (previous != Long.MIN_VALUE) {
                        assertTrue(t - previous >= interval, "gap " + (t - previous) + " < " + interval);
                    }
                    previous = t;
                    last = t;
                    steps++;
                }
            }
            assertTrue(steps <= 10_000 / interval + 1, "steps " + steps);
        }
    }

    @Test
    void clockRunningBackwardsRestartsCadence() {
        assertTrue(EmberStrike.stepDue(true, 1.0, 10, 5_000, 100));
    }

    @Test
    void levelCapIsFourPerSecond() {
        assertEquals(0, EmberStrike.strikeBudget(0));
        assertEquals(1, EmberStrike.strikeBudget(1));
        assertEquals(4, EmberStrike.strikeBudget(4));
        assertEquals(4, EmberStrike.strikeBudget(50));
        assertEquals(4, EmberStrike.LEVEL_CAP_PER_SECOND);
    }

    @Test
    void onlySkyExposedUnsafeColumns() {
        assertTrue(EmberStrike.columnStrikable(true, false));
        assertFalse(EmberStrike.columnStrikable(false, false), "no sky access");
        assertFalse(EmberStrike.columnStrikable(true, true), "Basalt Spire is immune");
        assertFalse(LightningRules.strikeAllowed(true));
    }

    @Test
    void intervalKeyDefaultsAndBounds() {
        var v = NerospaceConfig.schema().value("weather.emberStrikeIntervalTicks");
        assertEquals("100", v.defaultAsString());
        assertTrue(v.serverAuthoritative());
    }
}
