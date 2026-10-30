package za.co.neroland.nerospace.rocket;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerospace.rocket.StationRegistry.StationEntry;
import za.co.neroland.nerospace.world.station.StationAccess;
import za.co.neroland.nerospace.world.station.StationHooks;

/**
 * The Station Console's "Send visitor rocket home", installed into {@code StationHooks}: every riderless
 * <em>visitor</em> rocket parked at least 10 minutes on a dock or pad inside the station's claim box flies uncrewed
 * back to its own return pad (or a Landing Pod by the resolver in its origin dimension). A rocket is a visitor when
 * its rider could not {@code canUse} the station at arrival; the flag is a saved boolean, never an identity. The
 * owner's and members' own rockets, 1.6.0 rockets and any rocket with a rider are never moved.
 *
 * <p>A rocket stores no owner, so the visitor it belongs to cannot be told apart from other visitors. To never
 * strand anyone, nothing is sent while any player who cannot use the station is inside the claim box (they may be
 * the rocket's pilot, walking the station). When rockets are sent, every player in the claim box is told. Each send
 * logs the action and station slot only ({@link RocketEntity#sendHomeUncrewed}).</p>
 */
public final class StationVisitorSender {

    /** Ten minutes. */
    public static final long MIN_PARKED_TICKS = 12_000L;

    private StationVisitorSender() {
    }

    /**
     * Pure rule: a visitor rocket, riderless, parked long enough, standing on a landing block it blocks, and no
     * player who cannot use the station is still aboard it.
     */
    public static boolean eligible(boolean visitor, boolean hasRider, long parkedTicks, boolean onDock,
            boolean nonMemberAboard) {
        return visitor && !hasRider && parkedTicks >= MIN_PARKED_TICKS && onDock && !nonMemberAboard;
    }

    /**
     * Pure rule: whether a player inside the claim box holds the send back. Anyone who cannot use the station
     * (not owner, member or operator) does; a spectator cannot be stranded and does not.
     */
    public static boolean holdsSend(String owner, List<String> members, String player, boolean op, boolean spectator) {
        return !spectator && !StationAccess.canUse(owner, members, player, op);
    }

    /**
     * The {@code Visitor} flag set on an arriving rocket: only a station arrival whose rider could not use the
     * station. A planet arrival, the origin platform (no entry), an unowned station and an uncrewed send-home are
     * never visitors.
     */
    public static boolean isVisitor(boolean stationArrival, boolean hasEntry, boolean hasRider, boolean riderCanUse) {
        return stationArrival && hasEntry && hasRider && !riderCanUse;
    }

    /**
     * @return how many rockets were sent, or {@link StationHooks#VISITOR_ABOARD} when a player who cannot use the
     *         station is still inside the claim box (nothing is sent then).
     */
    public static int sendHome(ServerLevel level, StationEntry entry) {
        BlockPos c = entry.center();
        int r = StationRegistry.CLAIM_RADIUS;
        List<ServerPlayer> aboard = new ArrayList<>();
        boolean nonMemberAboard = false;
        for (ServerPlayer p : level.players()) {
            BlockPos at = p.blockPosition();
            if (Math.abs(at.getX() - c.getX()) > r || Math.abs(at.getZ() - c.getZ()) > r) {
                continue;
            }
            aboard.add(p);
            if (holdsSend(entry.owner(), entry.members(), p.getUUID().toString(), StationRegistry.isOp(p),
                    p.isSpectator())) {
                nonMemberAboard = true;
            }
        }
        if (nonMemberAboard) {
            return StationHooks.VISITOR_ABOARD;
        }
        AABB box = new AABB(c.getX() - r, level.getMinY(), c.getZ() - r, c.getX() + r + 1,
                level.getMaxY(), c.getZ() + r + 1);
        int sent = 0;
        for (RocketEntity rocket : level.getEntitiesOfClass(RocketEntity.class, box)) {
            if (rocket instanceof za.co.neroland.nerospace.route.CargoRocketEntity) {
                continue;
            }
            if (eligible(rocket.isVisitor(), !rocket.getPassengers().isEmpty(), rocket.parkedTicks(),
                    onDock(level, rocket.blockPosition()), nonMemberAboard)
                    && rocket.sendHomeUncrewed()) {
                sent++;
            }
        }
        if (sent > 0) {
            Component notice = Component.translatable("gui.nerospace.station_console.visitors_departed", sent);
            for (ServerPlayer p : aboard) {
                p.sendSystemMessage(notice, true);
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
