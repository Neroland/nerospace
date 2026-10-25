package za.co.neroland.nerospace.world.planet.mars.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * Martian Sandstone strata variant: a decorative block whose bands run along a horizontal axis. The axis follows the
 * placer's facing and turns with structure rotation, so the bedding lines up the way the builder laid it.
 */
public class StrataBlock extends Block {

    public static final MapCodec<StrataBlock> CODEC = BlockCodecs.simple(StrataBlock::new);

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    @SuppressWarnings("this-escape")
    public StrataBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    protected MapCodec<? extends StrataBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(AXIS, rotatedAxis(state.getValue(AXIS), rotation));
    }

    /** Quarter turns swap X and Z; a half turn (or none) keeps the axis. */
    public static Direction.Axis rotatedAxis(Direction.Axis axis, Rotation rotation) {
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            return axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        }
        return axis;
    }
}
