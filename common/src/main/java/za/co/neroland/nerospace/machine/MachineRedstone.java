package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Redstone control for the world-changing machines (Terraformer, …). A machine with NO adjacent
 * redstone component keeps the classic always-on behaviour — existing builds are untouched. Wiring any
 * signal source against it (a lever, a button, dust, …) turns that signal into a real switch: powered =
 * running, unpowered = idle.
 */
public final class MachineRedstone {

    private MachineRedstone() {
    }

    /** Whether redstone allows the machine at {@code pos} to run this tick (see class javadoc). */
    public static boolean allowsRun(Level level, BlockPos pos) {
        boolean wired = false;
        for (Direction side : Direction.values()) {
            if (level.getBlockState(pos.relative(side)).isSignalSource()) {
                wired = true;
                break;
            }
        }
        return !wired || level.hasNeighborSignal(za.co.neroland.nerospace.NerospaceCommon.requireNonNull(pos));
    }
}
