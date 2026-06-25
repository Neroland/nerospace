package za.co.neroland.nerospace.machine.quarry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * The quarry Landmark: a small marker post placed at the corners of the area to mine. Three forming an
 * L define the rectangle; the controller scans them on activation and consumes them. Carries a
 * {@link QuarryLandmarkBlockEntity} purely so the client can draw the projected marker lasers.
 */
public class QuarryLandmarkBlock extends BaseEntityBlock {

    public static final @org.jspecify.annotations.NonNull MapCodec<QuarryLandmarkBlock> CODEC = simpleCodec(QuarryLandmarkBlock::new);

    private static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 12.0D, 11.0D);

    public QuarryLandmarkBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<QuarryLandmarkBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuarryLandmarkBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.QUARRY_LANDMARK.get(),
                (lvl, pos, st, be) -> be.clientTick(lvl, pos, st));
    }
}
