package za.co.neroland.nerospace.world.planet.glacira;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

import za.co.neroland.nerospace.block.LoadedChunks;
import za.co.neroland.nerospace.world.planet.WeatherEffects;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherMachine;

/**
 * Diamond dust's weather behaviour: while the event runs, every 100 ticks each loaded chunk near a
 * player samples one random surface column and freezes it if it is an exposed water source. At most 32 cells per
 * chunk per event; the counters reset on start and end. Never loads a chunk; no block owns this behaviour.
 */
public final class DiamondDustFreeze implements WeatherEffects {

    private final Long2IntOpenHashMap frozenThisEvent = new Long2IntOpenHashMap();

    DiamondDustFreeze() {
    }

    @Override
    public void onStart(ServerLevel level, WeatherMachine weather) {
        this.frozenThisEvent.clear();
    }

    @Override
    public void onEnd(ServerLevel level, WeatherEvent ended) {
        this.frozenThisEvent.clear();
    }

    @Override
    public void onSecond(ServerLevel level, WeatherMachine weather) {
        if (level.getGameTime() % GlaciraNumbers.FREEZE_INTERVAL_TICKS >= WeatherMachine.STEP_TICKS) {
            return;
        }
        RandomSource random = level.getRandom();
        LoadedChunks.forEachNearPlayers(level, (cx, cz) -> {
            long key = ChunkPos.pack(cx, cz);
            int frozen = this.frozenThisEvent.get(key);
            if (frozen >= GlaciraNumbers.FREEZE_CAP_PER_EVENT) {
                return;
            }
            int x = (cx << 4) + random.nextInt(16);
            int z = (cz << 4) + random.nextInt(16);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).below();
            FluidState fluid = level.getFluidState(top);
            boolean source = fluid.is(FluidTags.WATER) && fluid.isSource()
                    && level.getBlockState(top).is(Blocks.WATER);
            if (GlaciraNumbers.freezes(source, level.canSeeSky(top.above()), frozen,
                    GlaciraNumbers.FREEZE_CAP_PER_EVENT)) {
                level.setBlock(top, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
                this.frozenThisEvent.put(key, frozen + 1);
            }
        });
    }
}
