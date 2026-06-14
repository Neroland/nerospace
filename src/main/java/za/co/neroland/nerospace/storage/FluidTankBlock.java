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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** Fluid Tank block — bucket in/out via right-click with a (filled/empty) bucket; bare hand reads out. */
public class FluidTankBlock extends AbstractStorageBlock {

    public static final MapCodec<FluidTankBlock> CODEC = simpleCodec(FluidTankBlock::new);

    public FluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<FluidTankBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    // The whole FluidUtil.interactWithFluidHandler family is deprecated-for-removal; the replacement
    // is hand-composing tryPlaceFluid/tryPickupFluid (item swap + sounds). Until that helper is
    // rebuilt, use the handler overload (cleaner than the location/Direction one) and suppress.
    @SuppressWarnings("removal")
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        // Pass the tank's fluid handler directly.
        if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            ResourceHandler<FluidResource> handler = tank.getFluidHandler();
            if (FluidUtil.interactWithFluidHandler(player, hand, pos, handler)) {
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            if (tank.storedFluid().isEmpty()) {
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.tank.empty"));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.tank.readout",
                        tank.storedAmount(), tank.capacity(),
                        tank.storedFluid().getFluid().getFluidType().getDescription()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
