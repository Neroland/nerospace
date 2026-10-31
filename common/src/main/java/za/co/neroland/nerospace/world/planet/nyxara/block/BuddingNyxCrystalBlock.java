package za.co.neroland.nerospace.world.planet.nyxara.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.BuddingRules;
import za.co.neroland.nerospace.world.planet.PlanetWeather;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.nyxara.NyxaraContent;

/**
 * Budding Nyx Crystal (Nyxara geodes): grows Nyx Crystal Clusters on its own faces. Each random tick, 1 time in 5
 * (4 in 5 while a flare runs), it picks a face: air becomes a stage-0 cluster facing out, a cluster already on that
 * face gains a stage up to 3. It never grows past its six faces and ignores light. Register with {@code randomTicks()}
 * and {@code pushReaction(DESTROY)}: a piston breaks it and it drops nothing without Silk Touch.
 */
public class BuddingNyxCrystalBlock extends HalfTransparentBlock {

    public static final MapCodec<BuddingNyxCrystalBlock> CODEC = BlockCodecs.simple(BuddingNyxCrystalBlock::new);

    public BuddingNyxCrystalBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends BuddingNyxCrystalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean flare = PlanetWeather.event(level) == WeatherEvent.FLARE;
        if (!BuddingRules.grows(random.nextInt(BuddingRules.ROLL), flare)) {
            return;
        }
        Direction face = Direction.getRandom(random);
        BlockPos target = pos.relative(face);
        BlockState at = level.getBlockState(target);
        NyxCrystalClusterBlock cluster = NyxaraContent.NYX_CRYSTAL_CLUSTER.get();
        int current;
        if (at.isAir()) {
            current = -1;
        } else if (at.is(cluster) && at.getValue(NyxCrystalClusterBlock.FACING) == face) {
            current = at.getValue(NyxCrystalClusterBlock.STAGE);
        } else {
            return;
        }
        int next = BuddingRules.next(current);
        if (next != current) {
            level.setBlockAndUpdate(target, cluster.defaultBlockState().setValue(NyxCrystalClusterBlock.FACING, face)
                    .setValue(NyxCrystalClusterBlock.STAGE, next));
        }
    }
}
