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

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * The Hydration Module block (DEEPER_TERRAFORM_DESIGN.md §3.1): a ticking machine backed by
 * {@link HydrationModuleBlockEntity} that melts glacite into hydration units for a TOUCHING
 * Terraformer's water stage.
 */
public class HydrationModuleBlock extends BaseEntityBlock {

    public static final @org.jspecify.annotations.NonNull MapCodec<HydrationModuleBlock> CODEC = simpleCodec(HydrationModuleBlock::new);
    /** The melt window faces the placer. Visual only. */
    public static final EnumProperty<Direction> FACING =
            NerospaceCommon.requireNonNull(BlockStateProperties.HORIZONTAL_FACING);

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public HydrationModuleBlock(Properties properties) {
        super(NerospaceCommon.requireNonNull(properties));
        this.registerDefaultState(NerospaceCommon.requireNonNull(
                this.stateDefinition.any().setValue(NerospaceCommon.requireNonNull(FACING), Direction.NORTH)));
    }

    @Override
    protected MapCodec<HydrationModuleBlock> codec() {
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
    public @NonNull BlockState getStateForPlacement(@NonNull BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return NerospaceCommon.requireNonNull(
                this.defaultBlockState().setValue(NerospaceCommon.requireNonNull(FACING), facing));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HydrationModuleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.HYDRATION_MODULE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof HydrationModuleBlockEntity be) {
            serverPlayer.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }
}
