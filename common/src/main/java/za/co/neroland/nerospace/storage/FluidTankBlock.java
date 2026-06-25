package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;

/** Fluid Tank block — holds a {@link FluidTankBlockEntity}; right-click empty-handed to read its contents. */
public class FluidTankBlock extends BaseEntityBlock {

    public static final MapCodec<FluidTankBlock> CODEC = simpleCodec(FluidTankBlock::new);

    public FluidTankBlock(Properties properties) {
        super(NerospaceCommon.requireNonNull(properties));
    }

    @Override
    protected MapCodec<FluidTankBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            if (tank.getTank().getFluid() == Fluids.EMPTY || tank.getTank().getAmount() <= 0) {
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.tank.empty"));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.tank.readout",
                        tank.getTank().getAmount(), tank.getTank().getCapacity(),
                        BuiltInRegistries.FLUID.getKey(
                                NerospaceCommon.requireNonNull(tank.getTank().getFluid())).getPath()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
