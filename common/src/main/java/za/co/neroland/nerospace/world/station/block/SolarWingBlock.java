package za.co.neroland.nerospace.world.station.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.world.station.StationContent;

/**
 * Solar Wing: a 2/16 panel that must be attached to Truss or Hull Plating. Output is a T2 ground
 * panel's peak × 2 (× 1.5 on an INDUSTRIAL station) while any horizontal neighbour is open void; it pushes into
 * adjacent energy receivers exactly as a solar panel does. Generates only in the orbital station dimension, whose
 * clock is fixed at full daylight.
 */
public class SolarWingBlock extends BaseEntityBlock {

    public static final MapCodec<SolarWingBlock> CODEC = BlockCodecs.simple(SolarWingBlock::new);

    private static final VoxelShape SHAPE = Block.box(0.0D, 7.0D, 0.0D, 16.0D, 9.0D, 16.0D);

    public SolarWingBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
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

    /** Attached = a Truss or Hull Plating on any side. */
    public static boolean supported(LevelReader level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.is(StationContent.TRUSS.get()) || n.is(StationContent.HULL_PLATING.get())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return supported(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarWingBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, StationContent.SOLAR_WING_BE.get(), (lvl, pos, st, be) -> be.tick(lvl, pos));
    }
}
