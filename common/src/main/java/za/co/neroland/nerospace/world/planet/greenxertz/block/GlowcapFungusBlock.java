package za.co.neroland.nerospace.world.planet.greenxertz.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.GrowthRules;
import za.co.neroland.nerospace.world.planet.greenxertz.world.GiantGlowcap;

/**
 * Glowcap Fungus: light 12, one {@code glow} particle per ~80 ticks, and bone meal grows a Giant Glowcap
 * 40 % of the time when {@link GrowthRules#giantFits} holds (5 air above, Xertz Soil below).
 */
public class GlowcapFungusBlock extends Block implements BonemealableBlock {

    public static final MapCodec<GlowcapFungusBlock> CODEC = BlockCodecs.simple(GlowcapFungusBlock::new);

    public static final int GLOW_ONE_IN = 80;

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 10, 12);

    public GlowcapFungusBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends GlowcapFungusBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(GLOW_ONE_IN) == 0) {
            level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
                    pos.getY() + 0.6, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.02, 0.0);
        }
    }

    //? if >=26.3 {
    /*@Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state,
            net.minecraft.world.level.block.BonemealSource source) {
        return GiantGlowcap.fits(level, pos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state,
            net.minecraft.world.level.block.BonemealSource source) {
        return random.nextFloat() < GrowthRules.BONEMEAL_CHANCE;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state,
            net.minecraft.world.level.block.BonemealSource source) {
        GiantGlowcap.grow(level, pos, random, false);
    }
    *///?} else {
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return GiantGlowcap.fits(level, pos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < GrowthRules.BONEMEAL_CHANCE;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        GiantGlowcap.grow(level, pos, random, false);
    }
    //?}
}
