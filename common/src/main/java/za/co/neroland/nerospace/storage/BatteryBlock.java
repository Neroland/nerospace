package za.co.neroland.nerospace.storage;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/** Battery block — holds a {@link BatteryBlockEntity} energy buffer; right-click empty-handed to read its charge. */
public class BatteryBlock extends BaseEntityBlock {

    public static final @org.jspecify.annotations.NonNull MapCodec<BatteryBlock> CODEC = simpleCodec(BatteryBlock::new);

    public BatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BatteryBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof BatteryBlockEntity battery) {
            serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.battery.readout",
                    battery.getEnergy().getAmount(), battery.getEnergy().getCapacity()));
        }
        return InteractionResult.SUCCESS;
    }
}
