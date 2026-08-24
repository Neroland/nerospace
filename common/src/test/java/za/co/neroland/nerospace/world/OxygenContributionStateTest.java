package za.co.neroland.nerospace.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Bounds, decay and persistence of external oxygen contributions. */
class OxygenContributionStateTest {

    private static final Identifier SOURCE = Identifier.fromNamespaceAndPath("neroagriculture", "greenhouse/1");
    private static final Identifier OTHER = Identifier.fromNamespaceAndPath("neroagriculture", "greenhouse/2");
    private static final BlockPos CENTER = new BlockPos(0, 64, 0);
    private static final long DURATION = 1_000L;

    private static OxygenContributionState roundTrip(OxygenContributionState state) {
        JsonElement encoded = OxygenContributionState.codec()
                .encodeStart(JsonOps.INSTANCE, state)
                .getOrThrow(error -> new AssertionError("encode failed: " + error));
        return OxygenContributionState.codec()
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(error -> new AssertionError("decode failed: " + error));
    }

    @Test
    @DisplayName("a fresh contribution reads at full strength at its centre")
    void fullStrengthAtCentre() {
        OxygenContributionState state = new OxygenContributionState();
        state.put(SOURCE, CENTER, 16, 10, 0L, DURATION);

        assertEquals(10, state.pressureAt(CENTER, 0L));
    }

    @Test
    @DisplayName("pressure decays with distance and is zero outside the radius")
    void decaysWithDistance() {
        OxygenContributionState state = new OxygenContributionState();
        state.put(SOURCE, CENTER, 16, 10, 0L, DURATION);

        int atCentre = state.pressureAt(CENTER, 0L);
        int halfway = state.pressureAt(CENTER.offset(8, 0, 0), 0L);

        assertTrue(halfway < atCentre, "pressure should fall off with distance");
        assertTrue(halfway > 0, "a point inside the radius should still read something");
        assertEquals(0, state.pressureAt(CENTER.offset(17, 0, 0), 0L), "outside the radius reads zero");
    }

    @Test
    @DisplayName("pressure decays with time and is zero once expired")
    void decaysWithTime() {
        OxygenContributionState state = new OxygenContributionState();
        state.put(SOURCE, CENTER, 16, 10, 0L, DURATION);

        assertTrue(state.pressureAt(CENTER, DURATION / 2) < state.pressureAt(CENTER, 0L));
        assertEquals(0, state.pressureAt(CENTER, DURATION), "an expired contribution reads zero");
        assertEquals(0, state.pressureAt(CENTER, DURATION + 1));
    }

    @Test
    @DisplayName("pressureAt is a pure read — it does not collect expired rows or mark the store dirty")
    void pressureAtIsPure() {
        OxygenContributionState state = new OxygenContributionState();
        state.put(SOURCE, CENTER, 16, 10, 0L, DURATION);
        // A freshly decoded store is clean, which gives us a reliable "was anything written?" baseline
        // without reaching for a setter.
        OxygenContributionState reloaded = roundTrip(state);
        assertFalse(reloaded.isDirty(), "a decoded store starts clean");

        assertEquals(0, reloaded.pressureAt(CENTER, DURATION + 1));

        assertFalse(reloaded.isDirty(), "a read on the tick path must not dirty the store");
    }

    @Test
    @DisplayName("expired rows are collected on the write paths")
    void pruneCollectsExpired() {
        OxygenContributionState state = new OxygenContributionState();
        state.put(SOURCE, CENTER, 16, 10, 0L, DURATION);

        assertEquals(1, state.size(0L));
        assertEquals(0, state.size(DURATION + 1), "size prunes as it counts");
    }

    @Test
    @DisplayName("re-contributing with the same source id replaces rather than stacks")
    void sameSourceReplaces() {
        OxygenContributionState state = new OxygenContributionState();
        state.put(SOURCE, CENTER, 16, 10, 0L, DURATION);
        state.put(SOURCE, CENTER, 16, 4, 0L, DURATION);

        assertEquals(1, state.size(0L));
        assertEquals(4, state.pressureAt(CENTER, 0L), "the later value wins outright");
    }

    @Test
    @DisplayName("an identical re-contribution reports no change")
    void identicalReContributionIsNoChange() {
        OxygenContributionState state = new OxygenContributionState();

        assertTrue(state.put(SOURCE, CENTER, 16, 10, 0L, DURATION));
        assertFalse(state.put(SOURCE, CENTER, 16, 10, 0L, DURATION));
    }

    @Test
    @DisplayName("separate sources stack, but the total stays clamped to the 0-15 scale")
    void totalIsClamped() {
        OxygenContributionState state = new OxygenContributionState();
        state.put(SOURCE, CENTER, 16, 15, 0L, DURATION);
        state.put(OTHER, CENTER, 16, 15, 0L, DURATION);

        assertEquals(15, state.pressureAt(CENTER, 0L));
    }

    @Test
    @DisplayName("removal takes effect immediately and reports whether anything was removed")
    void removal() {
        OxygenContributionState state = new OxygenContributionState();
        state.put(SOURCE, CENTER, 16, 10, 0L, DURATION);

        assertTrue(state.remove(SOURCE));
        assertFalse(state.remove(SOURCE), "removing twice is not a change");
        assertEquals(0, state.pressureAt(CENTER, 0L));
    }

    @Test
    @DisplayName("contributions survive a codec round trip")
    void survivesRoundTrip() {
        OxygenContributionState state = new OxygenContributionState();
        state.put(SOURCE, CENTER, 16, 10, 0L, DURATION);

        assertEquals(10, roundTrip(state).pressureAt(CENTER, 0L));
    }

    @Test
    @DisplayName("a nonsensical persisted row is skipped rather than dividing by zero")
    void malformedRowIsSkipped() {
        JsonElement handEdited = com.google.gson.JsonParser.parseString("""
                {"entries":[
                  {"source":"a:zero_radius","center":0,"radius":0,"strength":9,
                   "created_at":0,"expires_at":1000},
                  {"source":"a:inverted","center":0,"radius":8,"strength":9,
                   "created_at":1000,"expires_at":0}
                ]}""");

        OxygenContributionState reloaded = OxygenContributionState.codec()
                .parse(JsonOps.INSTANCE, handEdited)
                .getOrThrow(error -> new AssertionError("decode failed: " + error));

        assertEquals(0, reloaded.size(0L), "both malformed rows should be dropped");
    }
}
