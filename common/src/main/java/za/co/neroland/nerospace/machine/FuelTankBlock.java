package za.co.neroland.nerospace.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Fuel Tank: a fuel-storage machine that auto-fuels a rocket sitting on an adjacent launch pad.
 * Right-click with a fuel bucket/canister to deposit, with an empty bucket to draw a bucket back out,
 * or empty-handed to open its readout GUI. Emits a comparator signal scaled to its fill level.
 */
public class FuelTankBlock extends BaseEntityBlock {

    public static final MapCodec<FuelTankBlock> CODEC = simpleCodec(FuelTankBlock::new);

    public FuelTankBlock(Properties properties) {
        super(NerospaceCommon.requireNonNull(properties));
    }

    @Override
    protected MapCodec<FuelTankBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.FUEL_TANK.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FuelTankBlockEntity tank)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (stack.is(ModItems.ROCKET_FUEL_BUCKET.get())) {
            if (!level.isClientSide() && tank.tryFillContainer()) {
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
                playGlug(level, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.ROCKET_FUEL_CANISTER.get())) {
            if (!level.isClientSide() && tank.tryFillContainer()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                playGlug(level, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.BUCKET)) {
            if (!level.isClientSide() && tank.tryDrainBucket()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.getInventory().placeItemBackInInventory(
                            new ItemStack(ModItems.ROCKET_FUEL_BUCKET.get()));
                }
                playGlug(level, pos);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof FuelTankBlockEntity tank) {
            serverPlayer.openMenu(tank);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof FuelTankBlockEntity tank ? tank.comparatorSignal() : 0;
    }

    private static void playGlug(Level level, BlockPos pos) {
        level.playSound(null, NerospaceCommon.requireNonNull(pos), SoundEvents.BUCKET_EMPTY,
                SoundSource.BLOCKS, 0.7F, 1.0F);
    }
}
