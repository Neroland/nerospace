package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * Creative Fluid Tank block — right-click with a filled bucket to set the endless fluid (the bucket is
 * kept), sneak-right-click to clear, bare hand reads out.
 */
public class CreativeFluidTankBlock extends AbstractStorageBlock {

    public static final MapCodec<CreativeFluidTankBlock> CODEC = simpleCodec(CreativeFluidTankBlock::new);

    public CreativeFluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CreativeFluidTankBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeFluidTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        FluidStack contained = FluidUtil.getFirstStackContained(stack);
        if (!contained.isEmpty()) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos) instanceof CreativeFluidTankBlockEntity tank) {
                tank.setSource(FluidResource.of(contained.getFluid()));
                serverPlayer.sendSystemMessage(Component.translatable(
                        "block.nerospace.creative_tank.set", contained.getFluid().getFluidType().getDescription()));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CreativeFluidTankBlockEntity tank) {
            if (player.isShiftKeyDown()) {
                tank.setSource(FluidResource.EMPTY);
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.creative_tank.cleared"));
            } else if (tank.source().isEmpty()) {
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.creative_tank.unset"));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.creative_tank.readout",
                        tank.source().getFluid().getFluidType().getDescription()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
