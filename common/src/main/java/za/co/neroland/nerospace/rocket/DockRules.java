package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Pure Docking Ring rules. */
public final class DockRules {

    private DockRules() {
    }

    /** Where a docked rocket stands: {@code centre + (0.5, surfaceHeight, 0.5)}. */
    public static Vec3 dockPosition(BlockPos centre, double surfaceHeight) {
        return new Vec3(centre.getX() + 0.5D, centre.getY() + surfaceHeight, centre.getZ() + 0.5D);
    }

    /** A random tick (re-)registers the ring's node only when none is stored for its centre. */
    public static boolean shouldRegister(boolean registered) {
        return !registered;
    }
}
