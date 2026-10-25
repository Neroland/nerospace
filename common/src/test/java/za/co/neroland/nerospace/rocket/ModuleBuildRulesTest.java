package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.station.ModuleBuildRules;
import za.co.neroland.nerospace.world.station.ModuleBuildRules.Verdict;
import za.co.neroland.nerospace.world.station.ModuleEntry;
import za.co.neroland.nerospace.world.station.ModuleKind;
import za.co.neroland.nerospace.world.station.ModuleTemplates;
import za.co.neroland.nerospace.world.station.ObstructionRules.CellSample;
import za.co.neroland.nerospace.world.station.StationClass;

/** Idempotent build/repair planning: refusals, the cap, gates, and repair touching only damaged cells. */
class ModuleBuildRulesTest {

    static final BlockPos C = StationRegistry.centerFor(1);
    static final CellSample SOLID_OURS = new CellSample(false, false, true);

    static ModuleEntry entry(ModuleKind k, int i) {
        return new ModuleEntry(k, i, ModuleTemplates.anchor(k, i, C));
    }

    @Test
    void secondBuildOfAnIntactModulePlansNothing() {
        List<ModuleEntry> built = List.of(entry(ModuleKind.HABITAT_RING, 0));
        Set<BlockPos> filled = new HashSet<>();
        for (ModuleTemplates.Placement p : ModuleTemplates.template(ModuleKind.HABITAT_RING, 0, C)) {
            if (p.cell() != ModuleTemplates.Cell.AIR) {
                filled.add(p.pos());
            }
        }
        ModuleBuildRules.Plan plan = ModuleBuildRules.plan(built, StationClass.WAYSTATION, ModuleKind.HABITAT_RING, 0,
                C, p -> filled.contains(p) ? SOLID_OURS : CellSample.AIR, false);
        assertEquals(Verdict.REPAIR, plan.verdict());
        assertTrue(plan.placements().isEmpty());
    }

    @Test
    void damagedModuleRepairsOnlyTheAirCellsForFree() {
        List<ModuleEntry> built = List.of(entry(ModuleKind.HABITAT_RING, 0));
        List<ModuleTemplates.Placement> cells = ModuleTemplates.template(ModuleKind.HABITAT_RING, 0, C);
        Set<BlockPos> filled = new HashSet<>();
        for (ModuleTemplates.Placement p : cells) {
            if (p.cell() != ModuleTemplates.Cell.AIR) {
                filled.add(p.pos());
            }
        }
        BlockPos broken = cells.get(cells.size() - 1).pos();
        filled.remove(broken);
        ModuleBuildRules.Plan plan = ModuleBuildRules.plan(built, StationClass.WAYSTATION, ModuleKind.HABITAT_RING, 0,
                C, p -> filled.contains(p) ? SOLID_OURS : CellSample.AIR, false);
        assertEquals(Verdict.REPAIR, plan.verdict(), "repairs need no materials");
        assertEquals(1, plan.placements().size());
        assertEquals(broken, plan.placements().get(0).pos());
    }

    @Test
    void refusals() {
        assertEquals(Verdict.REFUSE_LEGACY, ModuleBuildRules.plan(List.of(), StationClass.WAYSTATION,
                ModuleKind.CORE_LEGACY, 0, C, p -> CellSample.AIR, true).verdict());
        assertEquals(Verdict.REFUSE_REQUIRES, ModuleBuildRules.plan(List.of(), StationClass.WAYSTATION,
                ModuleKind.HYDROPONICS_BAY, 0, C, p -> CellSample.AIR, true).verdict());
        assertEquals(Verdict.REFUSE_CLASS, ModuleBuildRules.plan(List.of(), StationClass.WAYSTATION,
                ModuleKind.OBSERVATORY, 0, C, p -> CellSample.AIR, true).verdict());
        assertEquals(Verdict.BUILD, ModuleBuildRules.plan(List.of(), StationClass.RESEARCH,
                ModuleKind.OBSERVATORY, 0, C, p -> CellSample.AIR, true).verdict());
        assertEquals(Verdict.REFUSE_COST, ModuleBuildRules.plan(List.of(), StationClass.WAYSTATION,
                ModuleKind.HABITAT_RING, 0, C, p -> CellSample.AIR, false).verdict());
        assertEquals(Verdict.REFUSE_INDEX, ModuleBuildRules.plan(List.of(), StationClass.WAYSTATION,
                ModuleKind.SOLAR_WING, 2, C, p -> CellSample.AIR, true).verdict());
        assertEquals(Verdict.REFUSE_OBSTRUCTED, ModuleBuildRules.plan(List.of(), StationClass.WAYSTATION,
                ModuleKind.HABITAT_RING, 0, C, p -> new CellSample(false, true, false), true).verdict());
    }

    @Test
    void ninthModuleIsRefused() {
        List<ModuleEntry> eight = new ArrayList<>();
        eight.add(entry(ModuleKind.CORE_LEGACY, 0));
        for (int i = 0; i < 8; i++) {
            eight.add(new ModuleEntry(ModuleKind.SOLAR_WING, 10 + i, C)); // eight capped entries
        }
        assertEquals(Verdict.REFUSE_MAX, ModuleBuildRules.plan(eight, StationClass.WAYSTATION,
                ModuleKind.HABITAT_RING, 0, C, p -> CellSample.AIR, true).verdict());
        assertEquals(ModuleBuildRules.RESULT_MAX, ModuleBuildRules.plan(eight, StationClass.WAYSTATION,
                ModuleKind.HABITAT_RING, 0, C, p -> CellSample.AIR, true).resultCode());
    }

    @Test
    void nextIndexAndDisabledRoundTrip() {
        List<ModuleEntry> one = List.of(entry(ModuleKind.SOLAR_WING, 0).withEnabled(false));
        assertEquals(1, ModuleBuildRules.nextIndex(one, ModuleKind.SOLAR_WING));
        assertEquals(-1, ModuleBuildRules.nextIndex(List.of(entry(ModuleKind.SOLAR_WING, 0),
                entry(ModuleKind.SOLAR_WING, 1)), ModuleKind.SOLAR_WING));
        var tag = ModuleEntry.LIST_CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, one).getOrThrow();
        assertEquals(one, ModuleEntry.LIST_CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow());
    }
}
