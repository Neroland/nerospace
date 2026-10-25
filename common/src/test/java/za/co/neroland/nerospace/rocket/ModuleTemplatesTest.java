package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.station.ModuleKind;
import za.co.neroland.nerospace.world.station.ModuleTemplates;
import za.co.neroland.nerospace.world.station.ModuleTemplates.Cell;

/** Module footprints: disjoint boxes, the documented block counts, and every cell close to the centre. */
class ModuleTemplatesTest {

    static final BlockPos C = StationRegistry.centerFor(3);

    record KI(ModuleKind kind, int index) {
    }

    static List<KI> buildable() {
        List<KI> out = new ArrayList<>();
        for (ModuleKind k : ModuleKind.values()) {
            if (k.buildable()) {
                for (int i = 0; i < k.maxCount(); i++) {
                    out.add(new KI(k, i));
                }
            }
        }
        return out;
    }

    @Test
    void boxesArePairwiseDisjoint() {
        List<KI> all = buildable();
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                ModuleTemplates.Box a = ModuleTemplates.box(all.get(i).kind(), all.get(i).index(), C);
                ModuleTemplates.Box b = ModuleTemplates.box(all.get(j).kind(), all.get(j).index(), C);
                assertFalse(a.intersects(b), all.get(i) + " overlaps " + all.get(j));
            }
        }
    }

    @Test
    void solidCellsStayInTheirOwnBox() {
        for (KI ki : buildable()) {
            ModuleTemplates.Box box = ModuleTemplates.box(ki.kind(), ki.index(), C);
            for (ModuleTemplates.Placement p : ModuleTemplates.template(ki.kind(), ki.index(), C)) {
                boolean truss = ki.kind() == ModuleKind.OBSERVATORY && p.cell() == Cell.TRUSS_Y;
                if (p.cell() == Cell.AIR || truss) {
                    continue; // doorways and the access column are cut into neighbours on purpose
                }
                assertTrue(box.contains(p.pos()), ki + " cell outside its box: " + p);
            }
        }
    }

    @Test
    void documentedCounts() {
        assertEquals(12, ModuleTemplates.count(ModuleTemplates.template(ModuleKind.HYDROPONICS_BAY, 0, C), Cell.TRAY));
        assertEquals(9, ModuleTemplates.count(ModuleTemplates.template(ModuleKind.GRAVITY_DECK, 0, C), Cell.PLATE));
        for (int i = 0; i < 2; i++) {
            List<ModuleTemplates.Placement> wing = ModuleTemplates.template(ModuleKind.SOLAR_WING, i, C);
            assertEquals(12, ModuleTemplates.count(wing, Cell.SOLAR_WING));
            assertEquals(8, ModuleTemplates.count(wing, Cell.TRUSS_Z));
        }
        assertEquals(9, ModuleTemplates.count(ModuleTemplates.template(ModuleKind.DOCKING_RING, 0, C), Cell.DOCK_RING));
        assertEquals(1, ModuleTemplates.count(ModuleTemplates.template(ModuleKind.OBSERVATORY, 0, C), Cell.TELESCOPE));
        assertTrue(ModuleTemplates.template(ModuleKind.CORE_LEGACY, 0, C).isEmpty(), "the legacy room is never built");
    }

    @Test
    void cellsStayNearTheCentreAndAreUnique() {
        for (KI ki : buildable()) {
            Set<BlockPos> seen = new HashSet<>();
            for (ModuleTemplates.Placement p : ModuleTemplates.template(ki.kind(), ki.index(), C)) {
                assertTrue(Math.abs(p.pos().getX() - C.getX()) <= 15 && Math.abs(p.pos().getZ() - C.getZ()) <= 15,
                        ki + " too far: " + p);
                assertTrue(p.pos().getY() >= C.getY() - 1 && p.pos().getY() <= C.getY() + 9, ki + " Y: " + p);
                assertTrue(seen.add(p.pos()), ki + " places a cell twice: " + p);
            }
        }
    }

    @Test
    void anchorsAreInsideTheirBoxes() {
        for (KI ki : buildable()) {
            assertTrue(ModuleTemplates.box(ki.kind(), ki.index(), C)
                    .contains(ModuleTemplates.anchor(ki.kind(), ki.index(), C)), ki.toString());
        }
        assertEquals(StationStructure.padCenter(C), ModuleTemplates.anchor(ModuleKind.DOCKING_RING, 0, C));
    }
}
