package za.co.neroland.nerospace.world.planet.nyxara.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.SpireChimeRules;
import za.co.neroland.nerospace.world.planet.nyxara.NyxaraContent;

/**
 * Crystal Spire (Nyxara band): translucent crystal columns, {@code SEGMENT} base / mid / tip (the tip is tapered).
 * Struck by a player or a projectile it chimes, pitched by how many spire blocks stand below it
 * ({@link SpireChimeRules}), at most once per 10 ticks per block (a scheduled no-op tick is the cooldown). Sound only.
 * The segment follows the column: a spire with spire (or a prism) above is base or mid, otherwise tip.
 */
public class CrystalSpireBlock extends HalfTransparentBlock {

    public static final MapCodec<CrystalSpireBlock> CODEC = BlockCodecs.simple(CrystalSpireBlock::new);

    public enum Segment implements StringRepresentable {
        BASE("base"), MID("mid"), TIP("tip");

        private final String name;

        Segment(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<Segment> SEGMENT = EnumProperty.create("segment", Segment.class);

    private static final VoxelShape TIP_SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 6, 16), Block.box(3, 6, 3, 13, 12, 13),
            Block.box(6, 12, 6, 10, 16, 10));

    @SuppressWarnings("this-escape")
    public CrystalSpireBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(SEGMENT, Segment.TIP));
    }

    protected MapCodec<? extends CrystalSpireBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEGMENT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SEGMENT) == Segment.TIP ? TIP_SHAPE : Shapes.block();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return segmentFor(context.getLevel(), context.getClickedPos());
    }

    //? if >=1.21.2 {
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
    //?} else {
    /*@Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
    *///?}
        if (direction.getAxis() == Direction.Axis.Y) {
            return segmentFor(level, pos);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    private BlockState segmentFor(LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        boolean continues = above.is(this) || above.is(NyxaraContent.NYX_PRISM.get());
        Segment segment = !continues ? Segment.TIP : level.getBlockState(pos.below()).is(this) ? Segment.MID : Segment.BASE;
        return defaultBlockState().setValue(SEGMENT, segment);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        chime(level, pos);
        super.attack(state, level, pos, player);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        chime(level, hit.getBlockPos());
        super.onProjectileHit(level, state, hit, projectile);
    }

    private void chime(Level level, BlockPos pos) {
        if (level.isClientSide() || level.getBlockTicks().hasScheduledTick(pos, this)) {
            return;
        }
        int below = 0;
        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (below < SpireChimeRules.MAX_COUNT && level.getBlockState(cursor.move(Direction.DOWN)).is(this)) {
            below++;
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F,
                SpireChimeRules.pitch(below));
        level.scheduleTick(pos, this, SpireChimeRules.COOLDOWN_TICKS);
    }
}
