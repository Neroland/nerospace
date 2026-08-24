package za.co.neroland.nerospace.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.api.PlanetId;

/** Persistence and erasure behaviour of the historical planet-visit store. */
class PlanetVisitStateTest {

    private static final PlanetId CINDARA =
            new PlanetId(Identifier.fromNamespaceAndPath("nerospace", "cindara"));
    private static final PlanetId GLACIRA =
            new PlanetId(Identifier.fromNamespaceAndPath("nerospace", "glacira"));

    private static PlanetVisitState roundTrip(PlanetVisitState state) {
        JsonElement encoded = PlanetVisitState.codec()
                .encodeStart(JsonOps.INSTANCE, state)
                .getOrThrow(error -> new AssertionError("encode failed: " + error));
        return PlanetVisitState.codec()
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(error -> new AssertionError("decode failed: " + error));
    }

    @Test
    @DisplayName("only the first visit to a planet counts as a change")
    void firstVisitOnly() {
        PlanetVisitState state = new PlanetVisitState();
        UUID player = UUID.randomUUID();

        assertTrue(state.record(player, CINDARA), "first visit should register");
        assertFalse(state.record(player, CINDARA), "a repeat visit must not fire again");
        assertTrue(state.record(player, GLACIRA), "a different planet is a new first visit");
    }

    @Test
    @DisplayName("visits survive a codec round trip")
    void survivesRoundTrip() {
        PlanetVisitState state = new PlanetVisitState();
        UUID player = UUID.randomUUID();
        state.record(player, CINDARA);
        state.record(player, GLACIRA);

        PlanetVisitState reloaded = roundTrip(state);

        assertTrue(reloaded.hasVisited(player, CINDARA));
        assertTrue(reloaded.hasVisited(player, GLACIRA));
        assertEquals(Set.of(CINDARA.asString(), GLACIRA.asString()), reloaded.export(player));
    }

    @Test
    @DisplayName("erasure drops the player's row and does not survive a reload")
    void erasureIsPersistent() {
        PlanetVisitState state = new PlanetVisitState();
        UUID erased = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        state.record(erased, CINDARA);
        state.record(other, CINDARA);

        state.forget(erased);

        assertFalse(state.hasVisited(erased, CINDARA));
        assertEquals(Set.of(), state.export(erased));
        assertTrue(state.hasVisited(other, CINDARA), "erasure must not touch anyone else");

        PlanetVisitState reloaded = roundTrip(state);
        assertFalse(reloaded.hasVisited(erased, CINDARA), "the erased row must not come back on reload");
        assertTrue(reloaded.hasVisited(other, CINDARA));
    }

    @Test
    @DisplayName("export returns an immutable copy, so a caller cannot mutate the store")
    void exportIsACopy() {
        PlanetVisitState state = new PlanetVisitState();
        UUID player = UUID.randomUUID();
        state.record(player, CINDARA);

        Set<String> exported = state.export(player);

        assertThrowsUnsupported(() -> exported.add("nerospace:greenxertz"));
        assertEquals(Set.of(CINDARA.asString()), state.export(player));
    }

    @Test
    @DisplayName("an unknown player has no visits rather than a null")
    void unknownPlayerIsEmpty() {
        PlanetVisitState state = new PlanetVisitState();

        assertEquals(Set.of(), state.export(UUID.randomUUID()));
        assertFalse(state.hasVisited(UUID.randomUUID(), CINDARA));
    }

    @Test
    @DisplayName("a malformed UUID row is skipped instead of failing the whole load")
    void malformedRowIsSkipped() {
        UUID good = UUID.fromString("00000000-0000-0000-0000-0000000000ab");
        JsonElement handEdited = com.google.gson.JsonParser.parseString("""
                {"visits":[
                  {"player":"not-a-uuid","planets":["nerospace:cindara"]},
                  {"player":"00000000-0000-0000-0000-0000000000ab","planets":["nerospace:cindara"]}
                ]}""");

        PlanetVisitState reloaded = PlanetVisitState.codec()
                .parse(JsonOps.INSTANCE, handEdited)
                .getOrThrow(error -> new AssertionError("decode failed: " + error));

        assertTrue(reloaded.hasVisited(good, CINDARA), "the good row must still load");
    }

    private static void assertThrowsUnsupported(Runnable action) {
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("expected the exported set to be immutable");
    }

    @Test
    @DisplayName("an empty store round-trips to an empty store")
    void emptyRoundTrip() {
        assertEquals(List.of(), List.copyOf(roundTrip(new PlanetVisitState()).export(UUID.randomUUID())));
    }
}
