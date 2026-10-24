package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * A block with a {@code PLACED} flag set only through {@link #getStateForPlacement} (002 §10.9): worldgen states are
 * {@code PLACED=false}. Base for Red Regolith (placed never drifts) and Lunar Basalt (only placed sinters).
 */
public class PlacedFlagBlock extends Block {

    public static final MapCodec<PlacedFlagBlock> CODEC = BlockCodecs.simple(PlacedFlagBlock::new);

    public static final BooleanProperty PLACED = BooleanProperty.create("placed");

    @SuppressWarnings("this-escape")
    public PlacedFlagBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(PLACED, Boolean.FALSE));
    }

    protected MapCodec<? extends PlacedFlagBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PLACED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(PLACED, Boolean.TRUE);
    }

    public static boolean playerPlaced(BlockState state) {
        return state.hasProperty(PLACED) && state.getValue(PLACED);
    }
}
