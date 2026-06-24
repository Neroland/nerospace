package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Gas Tank block — right-click reads out the stored gas. */
public class GasTankBlock extends AbstractStorageBlock {

    public static final MapCodec<GasTankBlock> CODEC = simpleCodec(GasTankBlock::new);

    public GasTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GasTankBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof GasTankBlockEntity tank) {
            if (tank.storedGas().isEmpty()) {
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.tank.empty"));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.tank.readout",
                        tank.storedAmount(), tank.capacity(), tank.storedGas().label()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
