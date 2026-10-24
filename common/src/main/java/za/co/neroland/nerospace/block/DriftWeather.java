package za.co.neroland.nerospace.block;

import java.util.function.Supplier;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.block.rules.DriftRules;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.world.planet.WeatherEffects;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherMachine;

/**
 * Dune drift hook (002 §1.2, §3): during the event, every {@link DriftRules#INTERVAL_TICKS} each loaded chunk near a
 * player picks one random surface column; if it is a drifting block it moves one block downwind (the storm direction
 * is fixed per event from the event seed). Count-conserving: the source becomes air in the same step the
 * destination is set. Moves per chunk per storm are capped (16, config may lower) and reset when the event ends.
 * Stage 3: {@code DriftWeather.register(WeatherEvent.DUST_STORM, ModBlocks.RED_REGOLITH::get)}.
 */
public final class DriftWeather implements WeatherEffects {

    private final Supplier<? extends DriftingBlock> block;
    private final Long2IntOpenHashMap movesThisStorm = new Long2IntOpenHashMap();

    private DriftWeather(Supplier<? extends DriftingBlock> block) {
        this.block = block;
    }

    public static void register(WeatherEvent event, Supplier<? extends DriftingBlock> block) {
        WeatherEffects.Registry.register(event, new DriftWeather(block));
    }

    @Override
    public void onStart(ServerLevel level, WeatherMachine weather) {
        this.movesThisStorm.clear();
    }

    @Override
    public void onEnd(ServerLevel level, WeatherEvent ended) {
        this.movesThisStorm.clear();
    }

    @Override
    public void onSecond(ServerLevel level, WeatherMachine weather) {
        if (level.getGameTime() % DriftRules.INTERVAL_TICKS >= WeatherMachine.STEP_TICKS) {
            return;
        }
        DriftingBlock drifting = this.block.get();
        Direction wind = Direction.from2DDataValue(DriftRules.stormDirection(weather.seed));
        int cap = NerospaceConfig.regolithDriftCap();
        RandomSource random = level.getRandom();
        LoadedChunks.forEachNearPlayers(level, (cx, cz) -> {
            long key = ChunkPos.pack(cx, cz);
            int moves = this.movesThisStorm.get(key);
            if (moves >= cap) {
                return;
            }
            int x = (cx << 4) + random.nextInt(16);
            int z = (cz << 4) + random.nextInt(16);
            BlockPos air = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
            BlockPos src = air.below();
            BlockState state = level.getBlockState(src);
            if (!drifting.drifts(state)) {
                return;
            }
            BlockPos dst = src.relative(wind);
            boolean downwindAir = level.getBlockState(dst).isAir();
            boolean supported = level.getBlockState(dst.below()).isFaceSturdy(level, dst.below(), Direction.UP);
            if (DriftRules.canDrift(true, false, level.getBlockState(air).isAir(), downwindAir, supported, moves, cap)
                    && level.isLoaded(dst)) {
                level.setBlock(dst, state, Block.UPDATE_ALL);
                level.removeBlock(src, false);
                this.movesThisStorm.put(key, moves + 1);
            }
        });
    }
}
