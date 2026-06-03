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

/** Creative Battery block — an endless energy source for testing. */
public class CreativeBatteryBlock extends AbstractStorageBlock {

    public static final MapCodec<CreativeBatteryBlock> CODEC = simpleCodec(CreativeBatteryBlock::new);

    public CreativeBatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CreativeBatteryBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeBatteryBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.creative_battery.readout"));
        }
        return InteractionResult.SUCCESS;
    }
}
