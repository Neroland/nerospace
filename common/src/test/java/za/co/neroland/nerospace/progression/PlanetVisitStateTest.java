package za.co.neroland.nerospace.progression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import za.co.neroland.nerospace.api.NerospacePlanets;

class PlanetVisitStateTest {

    @Test
    void visitsPersistAndPlayerErasureRemovesOnlyTheRequestedUuid() {
        UUID erased = UUID.randomUUID();
        UUID retained = UUID.randomUUID();
        PlanetVisitState state = new PlanetVisitState();
        state.record(erased, NerospacePlanets.CINDARA);
        state.record(retained, NerospacePlanets.GLACIRA);

        var json = PlanetVisitState.codec().encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        PlanetVisitState decoded = PlanetVisitState.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertTrue(decoded.hasVisited(erased, NerospacePlanets.CINDARA));

        decoded.forget(erased);
        assertFalse(decoded.hasVisited(erased, NerospacePlanets.CINDARA));
        assertTrue(decoded.hasVisited(retained, NerospacePlanets.GLACIRA));
    }
}
