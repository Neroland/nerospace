package za.co.neroland.nerospace.world.station;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.rocket.StationRegistry.StationEntry;

/**
 * The RESEARCH tempo source: timed progression counters a player owns advance by
 * {@code ceil(factor × Δticks)} where the factor is the station class's {@code starGuideSpeed} for a {@code canUse}
 * player inside that station's claim box, else 1.0. Timed counters (quests, visit dwell) read this.
 */
public final class StationTempo {

    private StationTempo() {
    }

    public static double factor(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !ModDimensions.STATION_LEVEL.equals(level.dimension())) {
            return 1.0;
        }
        StationEntry entry = StationRegistry.get(level.getServer()).entryAt(level.dimension(), player.blockPosition());
        if (entry == null || !StationRegistry.inClaimBox(entry, player.blockPosition())
                || !StationRegistry.canUse(entry, player)) {
            return 1.0;
        }
        return entry.stationClass().traits().starGuideSpeed();
    }

    /** Ticks a counter advances for {@code deltaTicks} of real time at {@code factor} (pure). */
    public static long advance(long deltaTicks, double factor) {
        return (long) Math.ceil(Math.max(1.0, factor) * Math.max(0L, deltaTicks));
    }
}
