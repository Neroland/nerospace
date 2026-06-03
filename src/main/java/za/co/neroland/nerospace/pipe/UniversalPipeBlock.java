package za.co.neroland.nerospace.pipe;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * The Universal Pipe (energy layer): a connection-aware transmitter that joins a {@link PipeNetwork}
 * with its neighbours and moves energy across it. Network membership is rebuilt lazily, so placing or
 * breaking a pipe (merging/splitting networks) needs no explicit block hooks — the network detects the
 * change on its next tick.
 *
 * <p>Right-click (empty hand) prints the segment's stored energy as a quick readout; the Configurator
 * sets per-face input/output modes.</p>
 */
public class UniversalPipeBlock extends BaseEntityBlock {

    public static final MapCodec<UniversalPipeBlock> CODEC = simpleCodec(UniversalPipeBlock::new);

    public UniversalPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<UniversalPipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UniversalPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.UNIVERSAL_PIPE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "block.nerospace.universal_pipe.energy", pipe.getEnergyHandler().getAmountAsInt()));
        }
        return InteractionResult.SUCCESS;
    }
}
