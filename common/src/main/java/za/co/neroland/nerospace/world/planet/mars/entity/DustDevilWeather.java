package za.co.neroland.nerospace.world.planet.mars.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerospace.block.WeatherScan;
import za.co.neroland.nerospace.block.rules.SpawnerRules;
import za.co.neroland.nerospace.world.planet.PlanetWeather;
import za.co.neroland.nerospace.world.planet.WeatherEffects;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherMachine;
import za.co.neroland.nerospace.world.planet.mars.MarsContent;

/**
 * Dust-storm hook for Dust Devil Pits. Every {@link SpawnerRules#INTERVAL_TICKS} ticks of a storm the loaded chunks
 * near players are queued through {@link WeatherScan} (palette-gated, at most 64 chunks per tick, at most
 * {@link DustDevilRules#PITS_PER_CHUNK} pits per chunk); each pit rolls 1-in-3 and spawns a devil when none is within
 * 32 blocks and fewer than two are in its 3x3 chunk area. When the storm ends every devil in the level is removed.
 * Holds no state between passes.
 */
public final class DustDevilWeather implements WeatherEffects {

    private DustDevilWeather() {
    }

    public static void register() {
        WeatherEffects.Registry.register(WeatherEvent.DUST_STORM, new DustDevilWeather());
    }

    @Override
    public void onSecond(ServerLevel level, WeatherMachine weather) {
        if (level.getGameTime() % SpawnerRules.INTERVAL_TICKS >= WeatherMachine.STEP_TICKS) {
            return;
        }
        WeatherScan.forEachLoadedBlock(level, state -> state.is(MarsContent.DUST_DEVIL_PIT.get()),
                DustDevilRules.PITS_PER_CHUNK, (pos, state) -> {
                    trySpawn(level, pos.immutable());
                    return true; // a visited pit counts against the per-chunk cap whether it spawned or not
                });
    }

    @Override
    public void onEnd(ServerLevel level, WeatherEvent ended) {
        List<? extends DustDevil> devils = level.getEntities(MarsContent.DUST_DEVIL.get(), e -> true);
        for (DustDevil devil : devils) {
            devil.discard();
        }
    }

    private static void trySpawn(ServerLevel level, BlockPos pit) {
        boolean storm = PlanetWeather.event(level) == WeatherEvent.DUST_STORM; // the scan drains later than it was queued
        if (!storm || !level.getBlockState(pit.above()).isAir()) {
            return;
        }
        boolean rolled = level.getRandom().nextInt(SpawnerRules.ONE_IN) == 0;
        if (!rolled) {
            return;
        }
        double r = SpawnerRules.EXCLUSION_RADIUS;
        int within = level.getEntitiesOfClass(DustDevil.class, new AABB(pit).inflate(r)).size();
        ChunkPos chunk = ChunkPos.containing(pit);
        AABB area = new AABB(chunk.getMinBlockX() - 16, level.getMinY(), chunk.getMinBlockZ() - 16,
                chunk.getMaxBlockX() + 17, level.getMaxY() + 1, chunk.getMaxBlockZ() + 17);
        int inArea = level.getEntitiesOfClass(DustDevil.class, area).size();
        if (DustDevilRules.maySpawn(true, within, inArea, true)) {
            DustDevil.spawnAt(level, pit);
        }
    }
}
