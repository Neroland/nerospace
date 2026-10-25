package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.station.ModuleKind;
import za.co.neroland.nerospace.world.station.ModuleTemplates;
import za.co.neroland.nerospace.world.station.ObstructionRules;
import za.co.neroland.nerospace.world.station.ObstructionRules.CellSample;

/** Never overwrite a player's block: only air or our own structure blocks inside the station's template union. */
class ObstructionRulesTest {

    static final BlockPos C = StationRegistry.centerFor(0);
    static final CellSample CHEST = new CellSample(false, true, false);
    static final CellSample FLOOR = new CellSample(false, false, true);

    @Test
    void chestBlocksWithItsPositionFirst() {
        List<ModuleTemplates.Placement> cells = ModuleTemplates.template(ModuleKind.HABITAT_RING, 0, C);
        BlockPos chest = cells.get(10).pos();
        Map<BlockPos, CellSample> world = new HashMap<>();
        world.put(chest, CHEST);
        ObstructionRules.Result r = ObstructionRules.scan(cells, p -> world.getOrDefault(p, CellSample.AIR),
                p -> ModuleTemplates.inUnion(C, p));
        assertEquals(1, r.blocked());
        assertEquals(chest, r.first());
    }

    @Test
    void stationFloorInsideTheUnionIsOurs() {
        assertTrue(ObstructionRules.writable(FLOOR, true));
        assertTrue(!ObstructionRules.writable(FLOOR, false), "station floor outside the union is a player's block");
        assertTrue(ObstructionRules.writable(CellSample.AIR, false));
        assertTrue(!ObstructionRules.writable(new CellSample(true, true, false), true), "air with a block entity");
        assertTrue(ModuleTemplates.inUnion(C, C.offset(-10, 1, 0)));
        assertTrue(!ModuleTemplates.inUnion(C, C.offset(-30, 1, 0)));
    }
}
