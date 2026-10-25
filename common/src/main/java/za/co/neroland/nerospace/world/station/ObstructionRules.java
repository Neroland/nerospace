package za.co.neroland.nerospace.world.station;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.Nullable;

/**
 * Never overwrite a player's block. A template cell may be written when the cell is air or
 * replaceable and holds no block entity, or when it holds a block Nerospace's own station templates place
 * ({@link #STRUCTURE_BLOCKS}) <b>and</b> lies inside this station's template union. Anything else — a chest, a
 * machine, cobblestone, a sibling mod's block — blocks the build. Pure: the caller samples the level.
 */
public final class ObstructionRules {

    /** Block ids the station templates place. */
    public static final Set<String> STRUCTURE_BLOCKS = Set.of(
            "nerospace:station_floor", "nerospace:station_wall", "minecraft:glass", "minecraft:sea_lantern",
            "nerospace:rocket_launch_pad", "nerospace:hull_plating", "nerospace:hull_window", "nerospace:truss",
            "nerospace:solar_wing", "nerospace:docking_ring", "nerospace:gravity_plate",
            "nerospace:hydroponics_tray", "nerospace:telescope");

    /**
     * What the level holds at one cell.
     *
     * @param airOrReplaceable air, or {@code canBeReplaced()} (snow layer, grass)
     * @param hasBlockEntity   the cell has a block entity
     * @param structureBlock   the block's id is in {@link #STRUCTURE_BLOCKS}
     */
    public record CellSample(boolean airOrReplaceable, boolean hasBlockEntity, boolean structureBlock) {

        public static final CellSample AIR = new CellSample(true, false, false);
    }

    /** Scan result: how many cells are blocked and the first one in template order. */
    public record Result(int blocked, @Nullable BlockPos first) {

        public boolean clear() {
            return this.blocked == 0;
        }
    }

    private ObstructionRules() {
    }

    /** Whether one cell may be written. */
    public static boolean writable(CellSample sample, boolean inUnion) {
        if (sample.airOrReplaceable() && !sample.hasBlockEntity()) {
            return true;
        }
        return sample.structureBlock() && inUnion;
    }

    /** Scans every template cell (doorway cuts included) against the level samples. */
    public static Result scan(List<ModuleTemplates.Placement> cells, Function<BlockPos, CellSample> sampler,
            Predicate<BlockPos> inUnion) {
        int blocked = 0;
        BlockPos first = null;
        for (ModuleTemplates.Placement p : cells) {
            if (!writable(sampler.apply(p.pos()), inUnion.test(p.pos()))) {
                if (first == null) {
                    first = p.pos();
                }
                blocked++;
            }
        }
        return new Result(blocked, first);
    }
}
