package za.co.neroland.nerospace.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import za.co.neroland.nerospace.world.OxygenContributionState;

/**
 * Bounded external oxygen-contribution API for plants and other optional providers. Source ids identify
 * gameplay sources and must never encode a player UUID, name, or other personal data.
 */
public final class NerospaceOxygen {

    public static final int MAX_RADIUS = 64;
    public static final int MAX_STRENGTH = 15;
    public static final long MAX_DURATION_TICKS = 20L * 60L * 60L;

    private NerospaceOxygen() {
    }

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

    public static boolean remove(ServerLevel level, Identifier source) {
        return level != null && source != null && OxygenContributionState.get(level).remove(source);
    }

    public static int pressureAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || !level.hasChunkAt(pos)) {
            return 0;
        }
        return OxygenContributionState.get(level).pressureAt(pos, level.getGameTime());
    }
}
