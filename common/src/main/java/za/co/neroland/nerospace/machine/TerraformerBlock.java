package za.co.neroland.nerospace.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;


import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * The Terraformer block (terraform design §2): a ticking machine backed by
 * {@link TerraformerBlockEntity} that advances an expanding terrain-conversion frontier while powered.
 */
public class TerraformerBlock extends BaseEntityBlock {

    public static final MapCodec<TerraformerBlock> CODEC = simpleCodec(TerraformerBlock::new);
    /** The core lens faces the placer. Visual only. */
    public static final EnumProperty<Direction> FACING =
            NerospaceCommon.requireNonNull(BlockStateProperties.HORIZONTAL_FACING);

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public TerraformerBlock(Properties properties) {
        super(NerospaceCommon.requireNonNull(properties));
        this.registerDefaultState(NerospaceCommon.requireNonNull(
                this.stateDefinition.any().setValue(NerospaceCommon.requireNonNull(FACING), Direction.NORTH)));
    }

    @Override
    protected MapCodec<TerraformerBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return NerospaceCommon.requireNonNull(
                this.defaultBlockState().setValue(NerospaceCommon.requireNonNull(FACING), facing));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TerraformerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.TERRAFORMER.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TerraformerBlockEntity be) {
            serverPlayer.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TerraformerBlockEntity be ? be.comparatorSignal() : 0;
    }
}
