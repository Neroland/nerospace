package za.co.neroland.nerospace.world.station;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.rocket.StationRegistry;

/**
 * The pure build/repair plan for one module. Given the station's current modules and class, the
 * requested kind/index and a sampler over the level, it decides whether to build, repair or refuse — and which
 * cells to write — before anything is consumed or placed. The level-side builder executes the plan.
 */
public final class ModuleBuildRules {

    /** Plan verdicts, in the order they are checked. */
    public enum Verdict {
        BUILD, REPAIR, REFUSE_LEGACY, REFUSE_INDEX, REFUSE_MAX, REFUSE_REQUIRES, REFUSE_CLASS, REFUSE_OBSTRUCTED,
        REFUSE_COST;

        public boolean refused() {
            return this != BUILD && this != REPAIR;
        }
    }

    /** Console result codes. */
    public static final int RESULT_OK = 0;
    public static final int RESULT_COST = 1;
    public static final int RESULT_CLASS = 2;
    public static final int RESULT_MAX = 3;
    public static final int RESULT_PERMISSION = 4;
    public static final int RESULT_OBSTRUCTED = 5;
    public static final int RESULT_REQUIRES = 6;
    public static final int RESULT_INVALID = 7;
    /** No result to show (the screen's idle state). */
    public static final int RESULT_NONE = 15;

    /** A plan: the verdict, the cells to write (empty when refused) and the first blocked position if any. */
    public record Plan(Verdict verdict, List<ModuleTemplates.Placement> placements, int blocked,
            @Nullable BlockPos firstBlocked) {

        public int resultCode() {
            return switch (this.verdict) {
                case BUILD, REPAIR -> RESULT_OK;
                case REFUSE_COST -> RESULT_COST;
                case REFUSE_CLASS -> RESULT_CLASS;
                case REFUSE_MAX -> RESULT_MAX;
                case REFUSE_OBSTRUCTED -> RESULT_OBSTRUCTED;
                case REFUSE_REQUIRES -> RESULT_REQUIRES;
                case REFUSE_LEGACY, REFUSE_INDEX -> RESULT_INVALID;
            };
        }
    }

    private ModuleBuildRules() {
    }

    private static Plan refuse(Verdict v) {
        return new Plan(v, List.of(), 0, null);
    }

    /**
     * Plans a build or repair.
     *
     * @param modules        the station's current module entries
     * @param stationClass   the station's class (Observatory gate)
     * @param kind           the module kind requested
     * @param index          per-kind index (Solar Wing 0/1; else 0)
     * @param centre         the station centre
     * @param sampler        what the level holds at a cell
     * @param costSatisfied  whether the builder holds the full cost (ignored for a repair)
     */
    public static Plan plan(List<ModuleEntry> modules, StationClass stationClass, ModuleKind kind, int index,
            BlockPos centre, Function<BlockPos, ObstructionRules.CellSample> sampler, boolean costSatisfied) {
        if (kind == ModuleKind.CORE_LEGACY) {
            return refuse(Verdict.REFUSE_LEGACY);
        }
        if (index < 0 || index >= kind.maxCount()) {
            return refuse(Verdict.REFUSE_INDEX);
        }
        List<ModuleTemplates.Placement> template = ModuleTemplates.template(kind, index, centre);
        if (find(modules, kind, index) != null) {
            // REPAIR: fill only cells that are air/replaceable with no block entity. Never clear a cell (a doorway
            // a player sealed stays sealed) and never replace a non-air block (a re-styled wall stays). Free.
            List<ModuleTemplates.Placement> fill = new ArrayList<>();
            for (ModuleTemplates.Placement p : template) {
                if (p.cell() == ModuleTemplates.Cell.AIR) {
                    continue;
                }
                ObstructionRules.CellSample s = sampler.apply(p.pos());
                if (s.airOrReplaceable() && !s.hasBlockEntity()) {
                    fill.add(p);
                }
            }
            return new Plan(Verdict.REPAIR, fill, 0, null);
        }
        if (kind == ModuleKind.CORE && hasKind(modules, ModuleKind.CORE_LEGACY)) {
            return refuse(Verdict.REFUSE_LEGACY);
        }
        if (kind.countsTowardCap() && capCount(modules) >= StationRegistry.MAX_MODULES) {
            return refuse(Verdict.REFUSE_MAX);
        }
        ModuleKind requires = kind.requires();
        if (requires != null && !hasKind(modules, requires)) {
            return refuse(Verdict.REFUSE_REQUIRES);
        }
        if (kind == ModuleKind.OBSERVATORY && !stationClass.traits().observatoryAllowed()) {
            return refuse(Verdict.REFUSE_CLASS);
        }
        ObstructionRules.Result scan = ObstructionRules.scan(template, sampler,
                p -> ModuleTemplates.inUnion(centre, p));
        if (!scan.clear()) {
            return new Plan(Verdict.REFUSE_OBSTRUCTED, List.of(), scan.blocked(), scan.first());
        }
        if (!costSatisfied) {
            return refuse(Verdict.REFUSE_COST);
        }
        return new Plan(Verdict.BUILD, template, 0, null);
    }

    @Nullable
    private static ModuleEntry find(List<ModuleEntry> modules, ModuleKind kind, int index) {
        for (ModuleEntry m : modules) {
            if (m.kind() == kind && m.index() == index) {
                return m;
            }
        }
        return null;
    }

    private static boolean hasKind(List<ModuleEntry> modules, ModuleKind kind) {
        for (ModuleEntry m : modules) {
            if (m.kind() == kind) {
                return true;
            }
        }
        return false;
    }

    private static int capCount(List<ModuleEntry> modules) {
        int n = 0;
        for (ModuleEntry m : modules) {
            if (m.kind().countsTowardCap()) {
                n++;
            }
        }
        return n;
    }

    /** The next free index for {@code kind} (Solar Wing 0 then 1), or {@code -1} when all are built. */
    public static int nextIndex(List<ModuleEntry> modules, ModuleKind kind) {
        for (int i = 0; i < kind.maxCount(); i++) {
            if (find(modules, kind, i) == null) {
                return i;
            }
        }
        return -1;
    }
}
