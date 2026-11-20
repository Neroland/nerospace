package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The configurable rocket ascent: duration, per-tick speed and the 2.3.1 baseline. */
class AscentMathTest {

    @Test
    void baselineIsTickIdenticalTo231() {
        assertEquals(100, AscentMath.duration(24));
        for (int tick = 0; tick < 100; tick++) {
            double t = (double) tick / 100;
            assertEquals(0.08D + 0.5D * (t * t), AscentMath.speed(tick, 100, 24), 0.0D, "tick " + tick);
        }
    }

    @Test
    void defaultIsFourTimesTheBaseline() {
        assertEquals(96, AscentMath.DEFAULT_HEIGHT);
        int duration = AscentMath.duration(96);
        assertEquals(159, duration);
        assertEquals(96.0D, AscentMath.totalClimb(duration, 96), 1.0D);
    }

    @Test
    void integratesToTheConfiguredHeightAcrossTheRange() {
        for (int height = AscentMath.MIN_HEIGHT; height <= AscentMath.MAX_HEIGHT; height++) {
            int duration = AscentMath.duration(height);
            assertEquals(height, AscentMath.totalClimb(duration, height), 1.0D, "height " + height);
        }
    }

    @Test
    void durationGrowsAndIsCapped() {
        int previous = 0;
        for (int height = AscentMath.MIN_HEIGHT; height <= AscentMath.MAX_HEIGHT; height++) {
            int duration = AscentMath.duration(height);
            assertTrue(duration >= previous, "monotonic at " + height);
            assertTrue(duration <= AscentMath.MAX_DURATION);
            previous = duration;
        }
        assertEquals(220, AscentMath.duration(256));
        assertEquals(AscentMath.duration(256), AscentMath.duration(100_000));
        assertEquals(100, AscentMath.duration(-5));
    }

    @Test
    void speedEasesInAndIsZeroOutsideTheAscent() {
        int duration = AscentMath.duration(256);
        double previous = 0.0D;
        for (int tick = 0; tick < duration; tick++) {
            double speed = AscentMath.speed(tick, duration, 256);
            assertTrue(speed > previous, "ease-in at " + tick);
            previous = speed;
        }
        assertTrue(previous < 3.0D, "peak speed stays bounded");
        assertEquals(0.0D, AscentMath.speed(-1, duration, 256));
        assertEquals(0.0D, AscentMath.speed(duration, duration, 256));
        assertEquals(0.0D, AscentMath.speed(0, 0, 96));
    }

    @Test
    void lowGravityBonusStaysBounded() {
        assertEquals(1.0D, AscentMath.gravityBonus(1.0D));
        assertEquals(1.5D, AscentMath.gravityBonus(0.0D));
        assertEquals(0.75D, AscentMath.gravityBonus(9.0D));
        assertEquals(1.5D, AscentMath.gravityBonus(-3.0D));
        // The tallest climb on the lightest world still ends well inside what a chunk column can hold.
        assertTrue(AscentMath.totalClimb(AscentMath.duration(256), 256) * 1.5D < 400.0D);
    }

    @Test
    void unknownDurationsFallBackToTheBaseline() {
        assertEquals(100, AscentMath.sanitizeDuration(0));
        assertEquals(100, AscentMath.sanitizeDuration(-1));
        assertEquals(100, AscentMath.sanitizeDuration(9_999));
        assertEquals(159, AscentMath.sanitizeDuration(159));
    }
}
