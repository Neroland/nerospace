package za.co.neroland.nerospace.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

class OxygenContributionStateTest {

    @Test
    void addRemoveAndLinearDecayAreBoundedAndPersistent() {
        Identifier source = Identifier.parse("test:plant");
        OxygenContributionState state = new OxygenContributionState();
        assertTrue(state.put(source, BlockPos.ZERO, 10, 10, 100, 100));
        assertEquals(10, state.pressureAt(BlockPos.ZERO, 100));
        assertEquals(5, state.pressureAt(BlockPos.ZERO, 150));
        assertEquals(0, state.pressureAt(new BlockPos(20, 0, 0), 150));

        var json = OxygenContributionState.codec().encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        OxygenContributionState decoded = OxygenContributionState.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(5, decoded.pressureAt(BlockPos.ZERO, 150));
        assertTrue(decoded.remove(source));
        assertEquals(0, decoded.pressureAt(BlockPos.ZERO, 150));
    }

    @Test
    void expiredContributionsPruneWithoutAttributionHistory() {
        OxygenContributionState state = new OxygenContributionState();
        state.put(Identifier.parse("test:temporary"), BlockPos.ZERO, 4, 15, 0, 20);
        assertEquals(0, state.pressureAt(BlockPos.ZERO, 20));
        assertEquals(0, state.size(20));
    }
}
