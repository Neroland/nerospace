package za.co.neroland.nerospace.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * The Hydration Module block (DEEPER_TERRAFORM_DESIGN.md §3.1): a ticking machine backed by
 * {@link HydrationModuleBlockEntity} that melts glacite into hydration units for a TOUCHING
 * Terraformer's water stage.
 */
public class HydrationModuleBlock extends BaseEntityBlock {

    public static final MapCodec<HydrationModuleBlock> CODEC = simpleCodec(HydrationModuleBlock::new);

    public HydrationModuleBlock(Properties properties) {
        super(properties);
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
