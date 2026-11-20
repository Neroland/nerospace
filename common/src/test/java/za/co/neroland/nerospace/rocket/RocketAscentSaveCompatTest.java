package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A rocket saved mid-ascent by 2.3.1 loads and completes its climb in 2.4.0. Entity NBT needs a level to decode,
 * so this reads a hand-built 2.3.1-shaped tag with the same keys, defaults and pure helpers as
 * {@link RocketEntity}, plus a source check that the entity really reads and writes them that way.
 */
class RocketAscentSaveCompatTest {

    /** What 2.3.1 wrote for a rocket {@code launchTicks} into its climb: no {@code AscentHeight}. */
    private static CompoundTag rocket231(int launchTicks) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Launching", true);
        tag.putInt("LaunchTicks", launchTicks);
        return tag;
    }

    private static int height(CompoundTag tag) {
        return AscentMath.clampHeight(TagReads.intOr(tag, "AscentHeight", AscentMath.BASE_HEIGHT));
    }

    @Test
    @DisplayName("a 2.3.1 mid-ascent rocket finishes on the 24-block, 100-tick baseline")
    void oldRocketCompletesOnTheBaseline() {
        CompoundTag tag = rocket231(40);
        int height = height(tag);
        int duration = AscentMath.duration(height);
        assertEquals(24, height);
        assertEquals(100, duration);
        int ticks = TagReads.intOr(tag, "LaunchTicks", 0);
        double climbed = 0.0D;
        int steps = 0;
        while (ticks < duration) {
            double t = (double) ticks / 100;
            assertEquals(0.08D + 0.5D * (t * t), AscentMath.speed(ticks, duration, height), 0.0D);
            climbed += AscentMath.speed(ticks, duration, height);
            ticks++;
            steps++;
        }
        assertEquals(60, steps, "the remaining 60 ticks, exactly as 2.3.1 would have run them");
        assertTrue(climbed > 0.0D);
    }

    @Test
    @DisplayName("a 2.4.0 rocket keeps the height its climb started with across a save")
    void newRocketKeepsItsHeight() {
        CompoundTag tag = rocket231(10);
        tag.putInt("AscentHeight", 96);
        assertEquals(96, height(tag));
        assertEquals(159, AscentMath.duration(height(tag)));
        tag.putInt("AscentHeight", 100_000);
        assertEquals(AscentMath.MAX_HEIGHT, height(tag), "a hand-edited value is clamped");
    }

    @Test
    @DisplayName("RocketEntity reads AscentHeight as optional and appends its synced duration last")
    void entitySourceMatches() throws Exception {
        String rocket = PodArrivalTest.source("RocketEntity.java");
        assertTrue(rocket.contains("input.getIntOr(\"AscentHeight\", AscentMath.BASE_HEIGHT)"));
        assertTrue(rocket.contains("output.putInt(\"AscentHeight\", this.ascentHeight)"));
        assertTrue(rocket.contains("input.getIntOr(\"LaunchTicks\", 0)"));
        // Entity-data ids are assigned in declaration order: the new field must come after every 2.3.1 field.
        int last231 = rocket.indexOf("DATA_PHASE_TICK =");
        int added = rocket.indexOf("DATA_ASCENT_TICKS =");
        assertTrue(last231 > 0 && added > last231, "DATA_ASCENT_TICKS is declared after DATA_PHASE_TICK");
        int define231 = rocket.indexOf("builder.define(DATA_PHASE_TICK");
        int defineAdded = rocket.indexOf("builder.define(DATA_ASCENT_TICKS");
        assertTrue(define231 > 0 && defineAdded > define231);
        assertEquals(100, RocketEntity.LAUNCH_DURATION, "the baseline and the flight-time budget are unchanged");
        assertEquals(168, Crew.FLIGHT_TICKS);
    }
}
