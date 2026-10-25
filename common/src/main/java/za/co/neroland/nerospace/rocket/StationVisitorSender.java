package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerospace.rocket.StationRegistry.StationEntry;

/**
 * The Station Console's "Send visitor rocket home" (005 G7, orchestrator ruling 5), installed into
 * {@code StationHooks}: every riderless rocket parked at least 10 minutes on a dock or pad inside the station's
 * claim box flies uncrewed back to its own return pad (or a Landing Pod by the resolver in its origin dimension).
 * A rocket with a rider is never moved. Rockets carry no owner, so nobody is messaged; each send logs the action
 * and station slot only ({@link RocketEntity#sendHomeUncrewed}).
 */
public final class StationVisitorSender {

    /** Ten minutes. */
    public static final long MIN_PARKED_TICKS = 12_000L;

    private StationVisitorSender() {
    }

    /** Pure rule: riderless, parked long enough, and standing on a landing block it blocks. */
    public static boolean eligible(boolean hasRider, long parkedTicks, boolean onDock) {
        return !hasRider && parkedTicks >= MIN_PARKED_TICKS && onDock;
    }

    public static int sendHome(ServerLevel level, StationEntry entry) {
        BlockPos c = entry.center();
        int r = StationRegistry.CLAIM_RADIUS;
        AABB box = new AABB(c.getX() - r, level.getMinY(), c.getZ() - r, c.getX() + r + 1,
                level.getMaxY(), c.getZ() + r + 1);
        int sent = 0;
        for (RocketEntity rocket : level.getEntitiesOfClass(RocketEntity.class, box)) {
            if (rocket instanceof za.co.neroland.nerospace.route.CargoRocketEntity) {
                continue;
            }
            if (eligible(!rocket.getPassengers().isEmpty(), rocket.parkedTicks(), onDock(level, rocket.blockPosition()))
                    && rocket.sendHomeUncrewed()) {
                sent++;
            }
        }
        return sent;
    }

    private static boolean onDock(ServerLevel level, BlockPos feet) {
        return isLanding(level.getBlockState(feet)) || isLanding(level.getBlockState(feet.below()));
    }

    private static boolean isLanding(BlockState s) {
        return s.getBlock() instanceof DockingRingBlock || s.getBlock() instanceof RocketLaunchPadBlock;
    }
}
