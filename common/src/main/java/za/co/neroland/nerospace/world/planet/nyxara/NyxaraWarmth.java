package za.co.neroland.nerospace.world.planet.nyxara;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.block.WeatherFlipBlock;
import za.co.neroland.nerospace.block.rules.WarmthRules;
import za.co.neroland.nerospace.world.planet.PlanetWeather;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * Dawnband COLD shield lookup for the hazard system: during a flare, a position within 4 blocks of
 * a {@code FLARING} Dawnband Stone is shielded from COLD. Outside a flare it returns false after one field read; during
 * one it scans the 9×9×9 cube (loaded chunks only) and stops at the first hit.
 */
public final class NyxaraWarmth {

    private static final int R = (int) WarmthRules.RADIUS;

    private NyxaraWarmth() {
    }

    public static boolean shieldedFromCold(ServerLevel level, BlockPos pos) {
        if (PlanetWeather.event(level) != WeatherEvent.FLARE) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -R; dx <= R; dx++) {
            for (int dy = -R; dy <= R; dy++) {
                for (int dz = -R; dz <= R; dz++) {
                    int distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared > R * R) {
                        continue;
                    }
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    BlockState s = level.getBlockState(cursor);
                    if (s.is(NyxaraContent.DAWNBAND_STONE.get())
                            && WarmthRules.shielded(s.getValue(WeatherFlipBlock.FLARING),
                                    Math.sqrt(distanceSquared),
                                    WarmthRules.RADIUS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
