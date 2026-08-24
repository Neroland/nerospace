package za.co.neroland.nerospace.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.api.TerraformRegion;
import za.co.neroland.nerospace.api.TerraformRequest;

/** Overlay persistence, baseline retention and lookup-by-position. */
class TerraformOverlayStateTest {

    private static final Identifier ID = Identifier.fromNamespaceAndPath("neroagriculture", "project/1");
    private static final Identifier OTHER = Identifier.fromNamespaceAndPath("neroagriculture", "project/2");
    private static final BlockPos CENTER = new BlockPos(0, 64, 0);

    private static TerraformRequest request(Identifier id, int radius, int stage, float progress) {
        return new TerraformRequest(id, CENTER, radius, stage, progress);
    }

    private static TerraformOverlayState roundTrip(TerraformOverlayState state) {
        JsonElement encoded = TerraformOverlayState.codec()
                .encodeStart(JsonOps.INSTANCE, state)
                .getOrThrow(error -> new AssertionError("encode failed: " + error));
        return TerraformOverlayState.codec()
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(error -> new AssertionError("decode failed: " + error));
    }

    @Test
    @DisplayName("applying stores the region and reports the change")
    void applyStores() {
        TerraformOverlayState state = new TerraformOverlayState();

        assertTrue(state.apply(request(ID, 32, 1, 0.5F), 0));

        TerraformRegion region = state.get(ID).orElseThrow();
        assertEquals(32, region.radius());
        assertEquals(1, region.stage());
        assertEquals(0.5F, region.progress());
    }

    @Test
    @DisplayName("re-applying an identical region is not a change")
    void identicalApplyIsNoChange() {
        TerraformOverlayState state = new TerraformOverlayState();
        state.apply(request(ID, 32, 1, 0.5F), 0);

        assertFalse(state.apply(request(ID, 32, 1, 0.5F), 0));
    }

    @Test
    @DisplayName("advancing a region keeps the baseline captured when it was created")
    void baselineIsCapturedOnce() {
        TerraformOverlayState state = new TerraformOverlayState();
        state.apply(request(ID, 32, 1, 0.0F), 2);

        // A later call passes a different baseline; the original must win, otherwise an overlay could
        // rewrite the physical progress it is meant to sit on top of.
        state.apply(request(ID, 32, 3, 1.0F), 0);

        assertEquals(2, state.baselineStage(ID));
    }

    @Test
    @DisplayName("rollback removes the region and is idempotent")
    void rollbackRemoves() {
        TerraformOverlayState state = new TerraformOverlayState();
        state.apply(request(ID, 32, 2, 0.5F), 0);

        assertTrue(state.rollback(ID));
        assertEquals(Optional.empty(), state.get(ID));
        assertFalse(state.rollback(ID), "rolling back twice is not a change");
    }

    @Test
    @DisplayName("lookup by position finds a covering region and ignores a distant one")
    void lookupByPosition() {
        TerraformOverlayState state = new TerraformOverlayState();
        state.apply(request(ID, 16, 1, 0.0F), 0);

        assertTrue(state.at(CENTER).isPresent());
        assertTrue(state.at(CENTER.offset(10, 0, 0)).isPresent());
        assertEquals(Optional.empty(), state.at(CENTER.offset(200, 0, 0)));
    }

    @Test
    @DisplayName("coverage is horizontal — a region reaches any altitude above and below its centre")
    void coverageIsHorizontal() {
        TerraformOverlayState state = new TerraformOverlayState();
        state.apply(request(ID, 16, 1, 0.0F), 0);

        // Far above and far below the centre, but within the horizontal radius: still covered. A
        // spherical test would switch the overlay off here while the physical chunk flag stayed on.
        assertTrue(state.at(CENTER.offset(0, 200, 0)).isPresent());
        assertTrue(state.at(CENTER.offset(0, -60, 0)).isPresent());
        assertTrue(state.at(CENTER.offset(15, 200, 0)).isPresent());
        // Outside the horizontal radius it is still not covered, at any height.
        assertEquals(Optional.empty(), state.at(CENTER.offset(17, 0, 0)));
    }

    @Test
    @DisplayName("when regions overlap the most advanced one wins")
    void mostAdvancedWins() {
        TerraformOverlayState state = new TerraformOverlayState();
        state.apply(request(ID, 32, 1, 0.9F), 0);
        state.apply(request(OTHER, 32, 3, 0.1F), 0);

        assertEquals(3, state.at(CENTER).orElseThrow().stage());
    }

    @Test
    @DisplayName("regions survive a codec round trip, baseline included")
    void survivesRoundTrip() {
        TerraformOverlayState state = new TerraformOverlayState();
        state.apply(request(ID, 32, 2, 0.25F), 1);

        TerraformOverlayState reloaded = roundTrip(state);

        assertEquals(2, reloaded.get(ID).orElseThrow().stage());
        assertEquals(1, reloaded.baselineStage(ID));
    }

    @Test
    @DisplayName("a persisted row whose id is not a valid Identifier is skipped, not thrown on")
    void malformedIdIsSkipped() {
        JsonElement handEdited = com.google.gson.JsonParser.parseString("""
                {"regions":[
                  {"id":"NOT AN ID","center":0,"radius":16,"baseline_stage":0,"stage":1,"progress":0.5},
                  {"id":"neroagriculture:project/1","center":0,"radius":16,"baseline_stage":0,
                   "stage":1,"progress":0.5}
                ]}""");

        TerraformOverlayState reloaded = TerraformOverlayState.codec()
                .parse(JsonOps.INSTANCE, handEdited)
                .getOrThrow(error -> new AssertionError("decode failed: " + error));

        // The good row still resolves, and reading the store no longer throws on the bad one. The
        // hand-edited rows encode center 0, i.e. the origin — not CENTER.
        assertTrue(reloaded.get(ID).isPresent());
        assertTrue(reloaded.at(BlockPos.ZERO).isPresent());
        assertEquals(Optional.empty(), reloaded.get(Identifier.fromNamespaceAndPath("a", "missing")));
    }

    @Test
    @DisplayName("a request outside the bounded limits is rejected outright")
    void requestBoundsAreEnforced() {
        assertThrowsIllegalArgument(() -> request(ID, 0, 1, 0.5F));
        assertThrowsIllegalArgument(() -> request(ID, TerraformRequest.MAX_RADIUS + 1, 1, 0.5F));
        assertThrowsIllegalArgument(() -> request(ID, 32, TerraformRequest.MAX_STAGE + 1, 0.5F));
        assertThrowsIllegalArgument(() -> request(ID, 32, 1, 1.5F));
        assertThrowsIllegalArgument(() -> request(ID, 32, 1, Float.NaN));
    }

    private static void assertThrowsIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected the out-of-bounds request to be rejected");
    }
}
