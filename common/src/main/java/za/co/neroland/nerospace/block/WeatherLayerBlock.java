package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.LayerRules;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.registry.ModTags;

/**
 * Snow-like weather layer (Ash Layer): vanilla snow-layer shape/stacking, plus weather accumulation through
 * {@link #accumulate}, a ×0.7 walk slow at 3+ layers, and melting only next to fire or lava (never by light).
 * Register it with {@code randomTicks()} and hooks {@link LayerWeather} to its planet's weather event.
 */
public class WeatherLayerBlock extends SnowLayerBlock {

    /**
     * Kept for registries that want an explicit codec. Not returned from {@code codec()}: SnowLayerBlock's override is
     * invariant ({@code MapCodec<SnowLayerBlock>}) on 26.1.2/26.2 and absent on 26.3, so the inherited one stands.
     */
    public static final MapCodec<WeatherLayerBlock> CODEC = BlockCodecs.simple(WeatherLayerBlock::new);

    public WeatherLayerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effects, boolean isPrecise) {
        float slow = LayerRules.slowFactor(state.getValue(LAYERS));
        if (slow < 1.0F) {
            entity.makeStuckInBlock(state, new Vec3(slow, 1.0, slow));
        }
        super.entityInside(state, level, pos, entity, effects, isPrecise);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int layers = state.getValue(LAYERS);
        int next = LayerRules.melt(layers, fireOrLavaAdjacent(level, pos));
        if (next == layers) {
            return; // vanilla snow melts by light; this layer only melts by heat
        }
        if (next <= 0) {
            level.removeBlock(pos, false);
        } else {
            level.setBlockAndUpdate(pos, state.setValue(LAYERS, next));
        }
    }

    private static boolean fireOrLavaAdjacent(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.is(Blocks.FIRE) || n.is(Blocks.SOUL_FIRE) || n.is(Blocks.LAVA) || n.is(Blocks.MAGMA_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds one weather layer at the top of the column {@code (x, z)} if the surface below is a sturdy full top that is
     * not airtight hull, and the layer is below the weather cap. Called by {@link LayerWeather} — one column per
     * chunk per {@link LayerRules#INTERVAL_TICKS}. Returns true when a block changed.
     */
    public boolean accumulate(ServerLevel level, BlockPos surfaceAir) {
        BlockState at = level.getBlockState(surfaceAir);
        int cap = NerospaceConfig.ashLayerCap();
        if (at.is(this)) {
            int layers = at.getValue(LAYERS);
            int next = LayerRules.nextLayers(layers, cap, true);
            if (next == layers) {
                return false;
            }
            return level.setBlockAndUpdate(surfaceAir, at.setValue(LAYERS, next));
        }
        if (!at.isAir() || !level.canSeeSky(surfaceAir)) {
            return false;
        }
        BlockPos below = surfaceAir.below();
        BlockState ground = level.getBlockState(below);
        if (ground.is(ModTags.Blocks.AIRTIGHT) || ground.hasBlockEntity()
                || !ground.isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        if (LayerRules.nextLayers(0, cap, true) == 0) {
            return false;
        }
        return level.setBlockAndUpdate(surfaceAir, defaultBlockState().setValue(LAYERS, 1));
    }
}
