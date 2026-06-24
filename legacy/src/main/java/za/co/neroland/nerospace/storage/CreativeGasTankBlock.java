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

import za.co.neroland.nerospace.gas.GasResource;

/** Creative Gas Tank block — an endless Oxygen source for testing. */
public class CreativeGasTankBlock extends AbstractStorageBlock {

    public static final MapCodec<CreativeGasTankBlock> CODEC = simpleCodec(CreativeGasTankBlock::new);

    public CreativeGasTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CreativeGasTankBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeGasTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "block.nerospace.creative_tank.readout", GasResource.OXYGEN.label()));
        }
        return InteractionResult.SUCCESS;
    }
}
