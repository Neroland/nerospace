package za.co.neroland.nerospace.world.station;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModTags;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.rocket.StationRegistry.StationEntry;
import za.co.neroland.nerospace.world.OxygenField;
import za.co.neroland.nerospace.world.OxygenFieldManager;
import za.co.neroland.nerospace.world.station.block.SolarWingBlockEntity;

/**
 * The level side of station modules: runs a {@link ModuleBuildRules} plan against the world,
 * consumes the cost atomically, writes the cells, registers the side effects (oxygen sources for airtight rooms, the
 * Docking Ring's node) and appends the {@link ModuleEntry}. Also hosts the first-2.0-visit {@code CORE_LEGACY}
 * record and the console's derived readouts (pressurised modules, power).
 */
public final class StationModules {

    /** The Docking Ring's registry id; looked up at runtime (the block lives in the rocket package). */
    public static final Identifier DOCKING_RING_ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID,
            "docking_ring");

    /**
     * Places a Docking Ring centred on {@code centre} and registers its {@code DOCK} node named {@code nodeName}.
     * The rocket package installs the real placer; until then {@link #genericDockPlacer} places the nine cells by
     * registry id and property names (the ring registers its node itself on its first random tick).
     */
    @FunctionalInterface
    public interface DockPlacer {
        boolean place(ServerLevel level, BlockPos centre, Direction facing, @Nullable String nodeName);
    }

    private static volatile DockPlacer dockPlacer = StationModules::genericDockPlacer;

    private StationModules() {
    }

    /** Installs the Docking Ring placer (called once by the rocket package's init). */
    public static void setDockPlacer(DockPlacer placer) {
        if (placer != null) {
            dockPlacer = placer;
        }
    }

    /** The registered Docking Ring block, or {@code null} when absent. */
    @Nullable
    public static Block dockingRingBlock() {
        return BuiltInRegistries.BLOCK.getOptional(DOCKING_RING_ID).orElse(null);
    }

    /** Places a Docking Ring through the installed placer; false (and a nameless warning) when unavailable. */
    public static boolean placeDock(ServerLevel level, BlockPos centre, @Nullable String nodeName) {
        if (dockingRingBlock() == null) {
            NerospaceCommon.LOGGER.warn("[Nerospace] Docking Ring block not registered; station dock skipped.");
            return false;
        }
        return dockPlacer.place(level, centre, Direction.EAST, nodeName);
    }

    private static boolean genericDockPlacer(ServerLevel level, BlockPos centre, Direction facing,
            @Nullable String nodeName) {
        Block ring = dockingRingBlock();
        if (ring == null) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = centre.offset(dx, 0, dz);
                BlockState current = level.getBlockState(p);
                if (current.is(ring) || !(current.isAir() || current.canBeReplaced())
                        || level.getBlockEntity(p) != null) {
                    continue;
                }
                BlockState state = withValue(ring.defaultBlockState(), "facing", facing.getSerializedName());
                state = withValue(state, "part", partName(dx, dz));
                level.setBlock(p, state, Block.UPDATE_ALL);
            }
        }
        return level.getBlockState(centre).is(ring);
    }

    /** Part name for a ring cell offset: {@code center}, {@code north}, {@code south_east}, … */
    static String partName(int dx, int dz) {
        String ns = dz < 0 ? "north" : dz > 0 ? "south" : "";
        String ew = dx > 0 ? "east" : dx < 0 ? "west" : "";
        if (ns.isEmpty() && ew.isEmpty()) {
            return "center";
        }
        return ns.isEmpty() ? ew : ew.isEmpty() ? ns : ns + "_" + ew;
    }

    private static BlockState withValue(BlockState state, String name, String value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property == null ? state : setByName(state, property, value);
    }

    private static <T extends Comparable<T>> BlockState setByName(BlockState state, Property<T> property,
            String value) {
        return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
    }

    // --- cell → block ---------------------------------------------------------------------------

    /** The block a template cell writes; {@code null} for cells handled elsewhere (dock, core). */
    @Nullable
    public static BlockState stateFor(ModuleTemplates.Cell cell) {
        return switch (cell) {
            case FLOOR -> ModBlocks.STATION_FLOOR.get().defaultBlockState();
            case STATION_WALL -> ModBlocks.STATION_WALL.get().defaultBlockState();
            case GLASS -> Blocks.GLASS.defaultBlockState();
            case LAMP -> Blocks.SEA_LANTERN.defaultBlockState();
            case PLATING -> StationContent.HULL_PLATING.get().defaultBlockState();
            case WINDOW -> StationContent.HULL_WINDOW.get().defaultBlockState();
            case TRUSS_X -> StationContent.TRUSS.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            case TRUSS_Y -> StationContent.TRUSS.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            case TRUSS_Z -> StationContent.TRUSS.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
            case SOLAR_WING -> StationContent.SOLAR_WING.get().defaultBlockState();
            case TRAY -> StationContent.HYDROPONICS_TRAY.get().defaultBlockState();
            case PLATE -> StationContent.GRAVITY_PLATE.get().defaultBlockState();
            case TELESCOPE -> StationContent.TELESCOPE.get().defaultBlockState();
            case AIR -> Blocks.AIR.defaultBlockState();
            case DOCK_RING, CORE -> null;
        };
    }

    /** What the level holds at one cell, for {@link ObstructionRules}. */
    public static ObstructionRules.CellSample sample(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean air = state.isAir() || state.canBeReplaced();
        boolean be = level.getBlockEntity(pos) != null;
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return new ObstructionRules.CellSample(air, be, ObstructionRules.STRUCTURE_BLOCKS.contains(id));
    }

    // --- build ----------------------------------------------------------------------------------

    /** Outcome of a console build click. */
    public record BuildOutcome(int resultCode, ModuleBuildRules.Verdict verdict, @Nullable BlockPos firstBlocked) {
    }

    /**
     * Builds (or repairs) one module for {@code payer} (null = free, e.g. founding). The caller has already checked
     * {@code canManage}. Order: plan → refuse, or consume the cost atomically → place → side effects →
     * append the entry.
     */
    public static BuildOutcome build(ServerLevel level, StationRegistry registry, StationEntry entry, ModuleKind kind,
            int index, @Nullable ServerPlayer payer) {
        BlockPos centre = entry.center();
        loadChunks(level, centre);
        Inventory inventory = payer == null || payer.getAbilities().instabuild ? null : payer.getInventory();
        boolean costOk = inventory == null || ModuleCosts.satisfiable(kind, inventoryCounts(inventory));
        ModuleBuildRules.Plan plan = ModuleBuildRules.plan(entry.modules(), entry.stationClass(), kind, index, centre,
                p -> sample(level, p), costOk);
        if (plan.verdict().refused()) {
            return new BuildOutcome(plan.resultCode(), plan.verdict(), plan.firstBlocked());
        }
        if (plan.verdict() == ModuleBuildRules.Verdict.BUILD && inventory != null) {
            consume(inventory, ModuleCosts.of(kind));
        }
        place(level, entry, kind, plan.placements(), plan.verdict() == ModuleBuildRules.Verdict.BUILD);
        if (kind.airtight()) {
            OxygenFieldManager.get(level).addSource(ModuleTemplates.oxygenCell(kind, index, centre));
        }
        if (plan.verdict() == ModuleBuildRules.Verdict.BUILD) {
            registry.addModule(entry.slot(), new ModuleEntry(kind, index, ModuleTemplates.anchor(kind, index, centre)));
        }
        return new BuildOutcome(ModuleBuildRules.RESULT_OK, plan.verdict(), null);
    }

    private static void place(ServerLevel level, StationEntry entry, ModuleKind kind,
            List<ModuleTemplates.Placement> cells, boolean fresh) {
        boolean dock = false;
        // Two passes: structure first, then cells that need support (wings on their truss).
        for (int pass = 0; pass < 2; pass++) {
            for (ModuleTemplates.Placement p : cells) {
                boolean late = p.cell() == ModuleTemplates.Cell.SOLAR_WING;
                if ((pass == 0) == late) {
                    continue;
                }
                if (p.cell() == ModuleTemplates.Cell.DOCK_RING) {
                    if (fresh) {
                        // Clear a legacy pad (structure blocks only — the plan's obstruction scan allowed it).
                        BlockState here = level.getBlockState(p.pos());
                        if (!here.isAir() && level.getBlockEntity(p.pos()) == null
                                && here.is(ModBlocks.ROCKET_LAUNCH_PAD.get())) {
                            level.setBlock(p.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                    dock = true;
                    continue;
                }
                BlockState state = stateFor(p.cell());
                if (state == null) {
                    continue;
                }
                level.setBlock(p.pos(), state, Block.UPDATE_ALL);
            }
        }
        if (dock) {
            placeDock(level, ModuleTemplates.anchor(ModuleKind.DOCKING_RING, 0, entry.center()),
                    entry.name() + " Dock");
        }
    }

    /** Loads the chunks a station's module union spans (centre ± 15 horizontally). */
    public static void loadChunks(ServerLevel level, BlockPos centre) {
        for (int x = centre.getX() - 16; x <= centre.getX() + 16; x += 16) {
            for (int z = centre.getZ() - 16; z <= centre.getZ() + 16; z += 16) {
                level.getChunk(x >> 4, z >> 4);
            }
        }
    }

    // --- inventory ------------------------------------------------------------------------------

    /** Item id → count over the player's main inventory + offhand. */
    public static Map<String, Integer> inventoryCounts(Inventory inventory) {
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(),
                        Integer::sum);
            }
        }
        return counts;
    }

    /** Removes exactly {@code cost} from the inventory (the caller checked it is satisfiable). */
    private static void consume(Inventory inventory, Map<String, Integer> cost) {
        for (Map.Entry<String, Integer> e : cost.entrySet()) {
            int left = e.getValue();
            for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(e.getKey())) {
                    int take = Math.min(left, stack.getCount());
                    stack.shrink(take);
                    left -= take;
                }
            }
        }
        inventory.setChanged();
    }

    // --- legacy room, life support, readouts ---------------------------------------------------

    /**
     * First 2.0 visit of a 1.x station: when the entry has no core module and a
     * Station Core stands at the centre, append {@code CORE_LEGACY} (never built, repaired or removed) and give the
     * old room one oxygen source — only if no source already covers it. Places and replaces no block. Idempotent.
     * @return whether the entry changed.
     */
    public static boolean ensureLegacyCore(ServerLevel level, StationRegistry registry, StationEntry entry) {
        if (entry.hasModule(ModuleKind.CORE_LEGACY) || entry.hasModule(ModuleKind.CORE)) {
            return false;
        }
        if (!level.getBlockState(entry.center()).is(ModBlocks.STATION_CORE.get())) {
            return false;
        }
        OxygenFieldManager oxygen = OxygenFieldManager.get(level);
        if (!roomHasSource(oxygen, entry.center())) {
            oxygen.addSource(ModuleTemplates.oxygenCell(ModuleKind.CORE_LEGACY, 0, entry.center()));
        }
        return registry.addModule(entry.slot(), new ModuleEntry(ModuleKind.CORE_LEGACY, 0, entry.center()));
    }

    /** Whether an oxygen source (a generator or our room cell) already sits inside the 7×7 room's interior. */
    static boolean roomHasSource(OxygenFieldManager oxygen, BlockPos centre) {
        int r = ModuleTemplates.ROOM_R;
        for (long packed : oxygen.sourceCells()) {
            BlockPos p = BlockPos.of(packed);
            if (Math.abs(p.getX() - centre.getX()) < r && Math.abs(p.getZ() - centre.getZ()) < r
                    && p.getY() >= centre.getY() && p.getY() <= centre.getY() + ModuleTemplates.WALL_H) {
                return true;
            }
        }
        return false;
    }

    /** Pressurised modules / airtight modules. */
    public static int[] pressurised(ServerLevel level, StationEntry entry) {
        OxygenFieldManager oxygen = OxygenFieldManager.get(level);
        int sealed = 0;
        int total = 0;
        for (ModuleEntry m : entry.modules()) {
            if (!m.kind().airtight()) {
                continue;
            }
            total++;
            BlockPos cell = ModuleTemplates.oxygenCell(m.kind(), m.index(), entry.center());
            if (oxygen.isBreathable(cell) && hullSealed(level, m, entry.center())) {
                sealed++;
            }
        }
        return new int[] { sealed, total };
    }

    /** Every wall/roof cell of the module's template still holds a sealing block (any loaded cell). */
    private static boolean hullSealed(ServerLevel level, ModuleEntry m, BlockPos centre) {
        if (m.kind() == ModuleKind.CORE_LEGACY) {
            return true; // the legacy room is not template-checked (it may have been re-styled since 1.x)
        }
        for (ModuleTemplates.Placement p : ModuleTemplates.template(m.kind(), m.index(), centre)) {
            ModuleTemplates.Cell c = p.cell();
            if (c != ModuleTemplates.Cell.PLATING && c != ModuleTemplates.Cell.WINDOW) {
                continue;
            }
            BlockState state = level.getBlockState(p.pos());
            if (!state.is(ModTags.Blocks.OXYGEN_SEALING) && !OxygenField.isAirtight(state)) {
                return false;
            }
        }
        return true;
    }

    /** Σ Solar Wing output (FE/t) over the station's wing modules. */
    public static int power(ServerLevel level, StationEntry entry) {
        int total = 0;
        for (ModuleEntry m : entry.modules()) {
            if (m.kind() != ModuleKind.SOLAR_WING) {
                continue;
            }
            for (ModuleTemplates.Placement p : ModuleTemplates.template(m.kind(), m.index(), entry.center())) {
                if (p.cell() == ModuleTemplates.Cell.SOLAR_WING && level.isLoaded(p.pos())
                        && level.getBlockEntity(p.pos()) instanceof SolarWingBlockEntity wing) {
                    total += wing.output();
                }
            }
        }
        return total;
    }

    /** Every oxygen source cell a station's airtight modules registered (abandonment cleanup). */
    public static List<BlockPos> oxygenCells(StationEntry entry) {
        List<BlockPos> out = new ArrayList<>();
        for (ModuleEntry m : entry.modules()) {
            if (m.kind().airtight()) {
                out.add(ModuleTemplates.oxygenCell(m.kind(), m.index(), entry.center()));
            }
        }
        return out;
    }
}
