package za.co.neroland.nerospace.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.block.rules.LayerRules;
import za.co.neroland.nerospace.world.planet.WeatherEffects;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherMachine;

/**
 * The weather hook for a {@link WeatherLayerBlock}: while the event runs, each loaded chunk with a player nearby gets
 * one random sky-exposed column per {@link LayerRules#INTERVAL_TICKS} (002 §3). Stage 3:
 * {@code LayerWeather.register(WeatherEvent.ASH_FALL, ModBlocks.ASH_LAYER::get)}.
 */
public final class LayerWeather implements WeatherEffects {

    private final Supplier<? extends WeatherLayerBlock> block;

    private LayerWeather(Supplier<? extends WeatherLayerBlock> block) {
        this.block = block;
    }

    public static void register(WeatherEvent event, Supplier<? extends WeatherLayerBlock> block) {
        WeatherEffects.Registry.register(event, new LayerWeather(block));
    }

    @Override
    public void onSecond(ServerLevel level, WeatherMachine weather) {
        // onSecond runs every 20 ticks; the layer cadence is 40.
        if (level.getGameTime() % LayerRules.INTERVAL_TICKS >= WeatherMachine.STEP_TICKS) {
            return;
        }
        WeatherLayerBlock layer = this.block.get();
        RandomSource random = level.getRandom();
        LoadedChunks.forEachNearPlayers(level, (cx, cz) -> {
            int x = (cx << 4) + random.nextInt(16);
            int z = (cz << 4) + random.nextInt(16);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
            BlockPos target = level.getBlockState(top.below()).is(layer) ? top.below() : top;
            layer.accumulate(level, target);
        });
    }
}
