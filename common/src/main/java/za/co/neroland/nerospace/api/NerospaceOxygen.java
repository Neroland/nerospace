package za.co.neroland.nerospace.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import za.co.neroland.nerospace.world.OxygenContributionState;

/**
 * Bounded external oxygen-contribution API for plants, greenhouses and other optional providers.
 *
 * <p><b>Public API — semver-stable.</b> A contribution is a bounded, expiring, position-centred oxygen
 * source that Nerospace folds into its own breathability and environment answers. It is deliberately
 * bounded on every axis ({@link #MAX_RADIUS}, {@link #MAX_STRENGTH}, {@link #MAX_DURATION_TICKS}) so no
 * integration can grant permanent free life support across a planet.</p>
 *
 * <h2>Source ids</h2>
 * A contribution is keyed by a caller-owned {@link Identifier}. Re-contributing with the same id replaces
 * the previous value, which makes the natural integration shape ("this controller currently produces N")
 * idempotent. <b>Source ids identify gameplay objects and must never encode a player UUID, name or any
 * other personal data</b> — Nerospace persists the id verbatim in world saved data, which has no erasure
 * path because it is not supposed to contain anything erasable.
 *
 * <h2>Privacy (POPIA/GDPR)</h2>
 * Nothing on this path is player-keyed. The stored row is source id, centre, radius, strength and two
 * timestamps — no actor is recorded, not even transiently.
 */
public final class NerospaceOxygen {

    /** Largest contribution radius, in blocks. */
    public static final int MAX_RADIUS = 64;

    /** Largest contribution strength, on the same 0-15 scale as the internal oxygen field. */
    public static final int MAX_STRENGTH = 15;

    /** Longest contribution lifetime — one hour of ticks. Contributions must be refreshed to persist. */
    public static final long MAX_DURATION_TICKS = 20L * 60L * 60L;

    /**
     * Oxygen concentration at or above which air counts as breathable without a suit. Shared by the
     * environment rules and {@code OxygenManager} so the threshold is stated exactly once.
     */
    public static final int BREATHABLE_PRESSURE = 6;

    private NerospaceOxygen() {
    }

    /**
     * Registers or replaces the contribution for {@code source}.
     *
     * @return true when the stored value actually changed; false when the arguments are out of bounds,
     *         the centre's chunk is not loaded, or the identical contribution was already present
     */
    public static boolean contribute(ServerLevel level, Identifier source, BlockPos center, int radius,
            int strength, long durationTicks) {
        if (level == null || source == null || center == null || !level.hasChunkAt(center)) {
            return false;
        }
        if (radius < 1 || radius > MAX_RADIUS || strength < 1 || strength > MAX_STRENGTH
                || durationTicks < 1 || durationTicks > MAX_DURATION_TICKS) {
            return false;
        }
        return OxygenContributionState.get(level).put(source, center.immutable(), radius, strength,
                level.getGameTime(), durationTicks);
    }

    /** Withdraws {@code source}'s contribution immediately. Returns true when one was actually removed. */
    public static boolean remove(ServerLevel level, Identifier source) {
        return level != null && source != null && OxygenContributionState.get(level).remove(source);
    }

    /**
     * Total contributed oxygen at a position, 0-15, after linear time and distance decay. A pure read:
     * expired contributions are ignored but not collected here, so this is safe to call on a tick path.
     */
    public static int pressureAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || !level.hasChunkAt(pos)) {
            return 0;
        }
        return OxygenContributionState.get(level).pressureAt(pos, level.getGameTime());
    }
}
