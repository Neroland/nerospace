package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Creative Fluid Tank block — an endless source of one configured fluid (defaults to rocket_fuel).
 * Right-click with a filled bucket to choose the source fluid (the bucket is kept), sneak-empty-hand to
 * clear, empty-hand to read it out.
 */
public class CreativeFluidTankBlock extends AbstractStorageBlock {

    public static final @org.jspecify.annotations.NonNull MapCodec<CreativeFluidTankBlock> CODEC = simpleCodec(CreativeFluidTankBlock::new);

    public CreativeFluidTankBlock(Properties properties) {
        super(NerospaceCommon.requireNonNull(properties));
    }

    @Override
    protected MapCodec<CreativeFluidTankBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeFluidTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof BucketItem bucket) {
            Fluid contained = bucket.getContent();
            if (contained != Fluids.EMPTY) {
                if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                        && level.getBlockEntity(pos) instanceof CreativeFluidTankBlockEntity tank) {
                    tank.setSource(contained);
                    serverPlayer.sendSystemMessage(Component.translatable(
                            "block.nerospace.creative_tank.set", fluidName(contained)));
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CreativeFluidTankBlockEntity tank) {
            if (player.isShiftKeyDown()) {
                tank.setSource(Fluids.EMPTY);
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.creative_tank.cleared"));
            } else if (tank.source() == Fluids.EMPTY) {
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.creative_tank.unset"));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.creative_tank.readout",
                        fluidName(tank.source())));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** A display name for a fluid, derived from its registry id (version-stable; no loader fluid-type API). */
    private static Component fluidName(Fluid fluid) {
        return Component.literal(BuiltInRegistries.FLUID.getKey(NerospaceCommon.requireNonNull(fluid)).getPath());
    }
}
