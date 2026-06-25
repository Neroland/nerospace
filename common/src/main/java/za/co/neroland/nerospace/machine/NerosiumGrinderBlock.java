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

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Nerosium Grinder block — directional, ticks + opens its {@link NerosiumGrinderBlockEntity}. */
public class NerosiumGrinderBlock extends BaseEntityBlock {

    public static final MapCodec<NerosiumGrinderBlock> CODEC = simpleCodec(NerosiumGrinderBlock::new);
    public static final EnumProperty<Direction> FACING =
            NerospaceCommon.requireNonNull(BlockStateProperties.HORIZONTAL_FACING);

    @SuppressWarnings("this-escape")
    public NerosiumGrinderBlock(Properties properties) {
        super(NerospaceCommon.requireNonNull(properties));
        this.registerDefaultState(java.util.Objects.requireNonNull(
                this.stateDefinition.any().setValue(NerospaceCommon.requireNonNull(FACING), Direction.NORTH)));
    }

    @Override
    protected MapCodec<NerosiumGrinderBlock> codec() {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NerosiumGrinderBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof NerosiumGrinderBlockEntity grinder) {
            serverPlayer.openMenu(grinder);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, java.util.Objects.requireNonNull(ModBlockEntities.NEROSIUM_GRINDER.get()),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }
}
