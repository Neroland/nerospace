package za.co.neroland.nerospace.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.api.TerraformRequest;

class TerraformOverlayStateTest {

    @Test
    void authorizationPersistenceAndRollbackAreFailClosed() {
        Identifier id = Identifier.parse("test:greenhouse_region");
        TerraformRequest request = new TerraformRequest(id, BlockPos.ZERO, 16, 2, 0.5F);
        TerraformOverlayState state = new TerraformOverlayState();

        assertFalse(state.apply(request, 0, false));
        assertTrue(state.get(id).isEmpty());
        assertTrue(state.apply(request, 0, true));
        assertEquals(2, state.at(new BlockPos(4, 0, 4)).orElseThrow().stage());

        var json = TerraformOverlayState.codec().encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        TerraformOverlayState decoded = TerraformOverlayState.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(0.5F, decoded.get(id).orElseThrow().progress());
        assertFalse(decoded.rollback(id, false));
        assertTrue(decoded.rollback(id, true));
        assertTrue(decoded.get(id).isEmpty());
    }
}
